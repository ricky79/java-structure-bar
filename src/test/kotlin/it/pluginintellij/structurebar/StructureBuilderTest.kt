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

import com.intellij.psi.PsiJavaFile
import com.intellij.testFramework.LoggedErrorProcessor
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase

class StructureBuilderTest : LightJavaCodeInsightFixtureTestCase() {

    override fun setUp() {
        // The unified IntelliJ IDEA distribution runs licensed Ultimate startup activities that cannot start
        // in headless tests. Their errors are unrelated to this plugin, so only those are ignored.
        LoggedErrorProcessor.executeWith<Exception>(IgnoreUltimateErrors) { super.setUp() }
    }

    private object IgnoreUltimateErrors : LoggedErrorProcessor() {
        override fun processError(category: String, message: String, details: Array<out String>, t: Throwable?) =
            if (message.contains("com.intellij.modules.ultimate")) emptySet() else super.processError(category, message, details, t)
    }

    private fun build(text: String): FileStructure {
        val file = myFixture.configureByText("Foo.java", text.trimIndent()) as PsiJavaFile
        return StructureBuilder.build(file)
    }

    private fun ClassEntry.memberLabels() = members.map { it.text + it.detail }

    fun testListsNestedClassesAlphabetically() {
        val structure = build(
            """
            public class Foo {
                static class Zeta { class Deep {} }
                interface Alpha {}
                enum Color { RED }
            }
            class Bar {}
            """
        )

        assertEquals(
            listOf("Bar", "Foo", "Foo.Alpha", "Foo.Color", "Foo.Zeta", "Foo.Zeta.Deep"),
            structure.classes.map { it.item.text },
        )
    }

    fun testListsFieldsConstructorsAndMethodsAlphabetically() {
        val structure = build(
            """
            public class Foo {
                private int count;
                public Foo() {}
                void zeta() {}
                String alpha(int a, String b) { return b; }
                static class Inner { void run() {} }
            }
            """
        )

        val foo = structure.classes.single { it.item.text == "Foo" }
        assertEquals(
            listOf("alpha(int, String) : String", "count : int", "Foo()", "zeta() : void"),
            foo.memberLabels(),
        )
    }

    fun testEnumShowsConstantsButNotImplicitMethods() {
        val structure = build(
            """
            enum Foo {
                RED, GREEN;
                int code() { return 0; }
            }
            """
        )

        assertEquals(listOf("code() : int", "GREEN", "RED"), structure.classes.single().memberLabels())
    }

    fun testRecordShowsComponentsButNotImplicitMembers() {
        val structure = build(
            """
            record Foo(int x, String name) {
                double length() { return 0; }
            }
            """
        )

        assertEquals(
            listOf("length() : double", "name : String", "x : int"),
            structure.classes.single().memberLabels(),
        )
    }

    fun testFindsInnermostClassAndMemberAtOffset() {
        val structure = build(
            """
            public class Foo {
                void outer() {}
                static class Inner {
                    void run() {
                        int x = 0;
                    }
                }
            }
            """
        )
        val offset = myFixture.editor.document.text.indexOf("int x")

        val cls = structure.classAt(offset)
        assertEquals("Foo.Inner", cls?.item?.text)
        assertEquals("run", structure.memberAt(cls!!, offset)?.text)
    }

    fun testOffsetOutsideAnyMemberHasNoMember() {
        val structure = build(
            """
            public class Foo {
                void outer() {}

                void other() {}
            }
            """
        )
        val offset = myFixture.editor.document.text.indexOf("\n\n") + 1

        val cls = structure.classAt(offset)
        assertEquals("Foo", cls?.item?.text)
        assertNull(structure.memberAt(cls!!, offset))
    }

    fun testOffsetBeforeAnyClassHasNoClass() {
        val structure = build(
            """
            import java.util.List;

            class Foo {}
            """
        )

        assertNull(structure.classAt(0))
    }
}
