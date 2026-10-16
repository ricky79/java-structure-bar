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
