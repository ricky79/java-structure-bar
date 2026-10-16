/*
 * Java Structure Bar - a navigation bar for Java editors in IntelliJ IDEA
 * Copyright (C) 2026 Riccardo Oliosi
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package it.pluginintellij.structurebar

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.util.Iconable
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiField
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiRecordComponent
import com.intellij.psi.SyntheticElement
import com.intellij.psi.impl.light.LightElement
import javax.swing.Icon

/** One entry of a dropdown: [text] is shown normally, [detail] in grey right after it. */
data class StructureItem(
    val text: String,
    val detail: String,
    val icon: Icon?,
    val range: TextRange,
    val navigationOffset: Int,
)

class ClassEntry(val item: StructureItem, val members: List<StructureItem>)

class FileStructure(val classes: List<ClassEntry>) {

    /** The innermost listed class whose declaration contains [offset]. */
    fun classAt(offset: Int): ClassEntry? =
        classes.filter { it.item.range.containsOffset(offset) }.minByOrNull { it.item.range.length }

    fun memberAt(cls: ClassEntry, offset: Int): StructureItem? =
        cls.members.filter { it.range.containsOffset(offset) }.minByOrNull { it.range.length }

    companion object {
        val EMPTY = FileStructure(emptyList())
    }
}

object StructureBuilder {

    private val byLabel = compareBy<StructureItem, String>(String.CASE_INSENSITIVE_ORDER) { it.text }
        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.detail }

    fun build(file: PsiJavaFile): FileStructure {
        val entries = mutableListOf<ClassEntry>()

        fun visit(cls: PsiClass, outerName: String?) {
            val name = cls.name ?: return
            val qualifiedName = if (outerName == null) name else "$outerName.$name"
            val item = item(cls, qualifiedName, "") ?: return
            entries += ClassEntry(item, members(cls))
            cls.innerClasses.forEach { visit(it, qualifiedName) }
        }

        file.classes.forEach { visit(it, null) }
        return FileStructure(entries.sortedWith(compareBy(byLabel) { it.item }))
    }

    private fun members(cls: PsiClass): List<StructureItem> {
        val items = mutableListOf<StructureItem>()
        cls.recordComponents.mapNotNullTo(items) { it.toItem() }
        cls.fields.mapNotNullTo(items) { it.toItem() }
        cls.methods.mapNotNullTo(items) { it.toItem() }
        return items.sortedWith(byLabel)
    }

    private fun PsiRecordComponent.toItem() = item(this, name, " : ${type.presentableText}")

    private fun PsiField.toItem() =
        if (this is PsiEnumConstant) item(this, name, "") else item(this, name, " : ${type.presentableText}")

    private fun PsiMethod.toItem(): StructureItem? {
        val parameters = parameterList.parameters.joinToString(", ", "(", ")") { it.type.presentableText }
        val returnType = returnType?.let { " : ${it.presentableText}" } ?: ""
        return item(this, name, parameters + returnType)
    }

    /** Returns null for elements that are not written in the source, like implicit enum or record members. */
    private fun item(element: PsiElement, text: String, detail: String): StructureItem? {
        if (element is SyntheticElement || element is LightElement) return null
        val range = element.textRange ?: return null
        return StructureItem(text, detail, iconOf(element), range, element.textOffset)
    }

    private fun iconOf(element: PsiElement): Icon? =
        try {
            element.getIcon(Iconable.ICON_FLAG_VISIBILITY)
        } catch (_: IndexNotReadyException) {
            when (element) {
                is PsiClass -> AllIcons.Nodes.Class
                is PsiMethod -> AllIcons.Nodes.Method
                else -> AllIcons.Nodes.Field
            }
        }
}
