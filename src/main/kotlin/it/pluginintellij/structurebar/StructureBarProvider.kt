package it.pluginintellij.structurebar

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationProvider
import java.util.function.Function
import javax.swing.JComponent

private val STRUCTURE_BAR = Key.create<StructureBar>("it.pluginintellij.structurebar.StructureBar")

/** Puts a [StructureBar] at the top of every editor showing a Java source file. */
class StructureBarProvider : EditorNotificationProvider, DumbAware {

    override fun collectNotificationData(project: Project, file: VirtualFile): Function<in FileEditor, out JComponent?>? {
        if (file.fileType != JavaFileType.INSTANCE) return null

        return Function { fileEditor ->
            val textEditor = fileEditor as? TextEditor ?: return@Function null
            // Notifications can be refreshed many times for the same editor: reuse the bar instead of stacking new ones.
            fileEditor.getUserData(STRUCTURE_BAR) ?: StructureBar(project, textEditor.editor).also { bar ->
                Disposer.register(fileEditor, bar)
                fileEditor.putUserData(STRUCTURE_BAR, bar)
            }
        }
    }
}
