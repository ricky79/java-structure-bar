package it.pluginintellij.structurebar

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiJavaFile
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.Alarm
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.JBUI
import java.awt.GridLayout
import javax.swing.DefaultComboBoxModel
import javax.swing.JList
import javax.swing.JPanel

private const val REBUILD_DELAY_MS = 300
private const val VISIBLE_ROWS = 30

/** The two dropdowns shown above a Java editor: classes on the left, members of the selected class on the right. */
class StructureBar(private val project: Project, private val editor: Editor) :
    JPanel(GridLayout(1, 2, JBUI.scale(6), 0)), Disposable {

    private val classCombo = ComboBox<ClassEntry>()
    private val memberCombo = ComboBox<StructureItem>()
    private val rebuildAlarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)

    private var structure = FileStructure.EMPTY
    private var membersShownFor: ClassEntry? = null

    /** True while the dropdowns are changed by code, so their listeners don't move the caret. */
    private var syncing = false

    init {
        border = JBUI.Borders.compound(JBUI.Borders.customLineBottom(JBColor.border()), JBUI.Borders.empty(3, 6))

        classCombo.renderer = ItemRenderer { it.item }
        memberCombo.renderer = ItemRenderer { it }
        // Fixed preferred width: avoids measuring every entry, the grid decides the real width anyway.
        val prototype = StructureItem("X".repeat(30), "", null, TextRange.EMPTY_RANGE, 0)
        classCombo.prototypeDisplayValue = ClassEntry(prototype, emptyList())
        memberCombo.prototypeDisplayValue = prototype
        classCombo.maximumRowCount = VISIBLE_ROWS
        memberCombo.maximumRowCount = VISIBLE_ROWS

        classCombo.addActionListener { if (!syncing) onClassChosen() }
        memberCombo.addActionListener { if (!syncing) onMemberChosen() }

        add(classCombo)
        add(memberCombo)

        editor.caretModel.addCaretListener(object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) = syncWithCaret()
        }, this)
        editor.document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                rebuildAlarm.cancelAllRequests()
                rebuildAlarm.addRequest(::rebuild, REBUILD_DELAY_MS)
            }
        }, this)

        rebuild()
    }

    private fun rebuild() {
        ReadAction.nonBlocking<FileStructure> {
            val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) as? PsiJavaFile
            if (file == null) FileStructure.EMPTY else StructureBuilder.build(file)
        }
            .withDocumentsCommitted(project)
            .expireWith(this)
            .coalesceBy(this)
            .finishOnUiThread(ModalityState.any()) { newStructure ->
                structure = newStructure
                withSyncing { classCombo.model = DefaultComboBoxModel(newStructure.classes.toTypedArray()) }
                syncWithCaret()
            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    /** Selects the class and member that contain the caret. */
    private fun syncWithCaret() {
        val offset = editor.caretModel.offset
        val cls = structure.classAt(offset) ?: structure.classes.firstOrNull()
        withSyncing {
            showClass(cls)
            memberCombo.selectedItem = cls?.let { structure.memberAt(it, offset) }
        }
    }

    private fun showClass(cls: ClassEntry?) {
        classCombo.selectedItem = cls
        if (membersShownFor === cls) return
        membersShownFor = cls
        memberCombo.model = DefaultComboBoxModel((cls?.members ?: emptyList()).toTypedArray())
    }

    private fun onClassChosen() {
        val cls = classCombo.selectedItem as? ClassEntry ?: return
        withSyncing {
            showClass(cls)
            memberCombo.selectedItem = null
        }
        navigateTo(cls.item.navigationOffset)
    }

    private fun onMemberChosen() {
        val member = memberCombo.selectedItem as? StructureItem ?: return
        navigateTo(member.navigationOffset)
    }

    private fun navigateTo(offset: Int) {
        val caretModel = editor.caretModel
        caretModel.removeSecondaryCarets()
        caretModel.moveToOffset(offset.coerceIn(0, editor.document.textLength))
        editor.selectionModel.removeSelection()
        editor.scrollingModel.scrollToCaret(ScrollType.CENTER_UP)
        IdeFocusManager.getInstance(project).requestFocus(editor.contentComponent, true)
    }

    private inline fun withSyncing(action: () -> Unit) {
        val previous = syncing
        syncing = true
        try {
            action()
        } finally {
            syncing = previous
        }
    }

    override fun dispose() = Unit

    private class ItemRenderer<T>(private val toItem: (T) -> StructureItem) : ColoredListCellRenderer<T>() {
        override fun customizeCellRenderer(list: JList<out T>, value: T?, index: Int, selected: Boolean, hasFocus: Boolean) {
            val item = value?.let(toItem) ?: return
            icon = item.icon
            append(item.text)
            if (item.detail.isNotEmpty()) append(item.detail, SimpleTextAttributes.GRAYED_ATTRIBUTES)
        }
    }
}
