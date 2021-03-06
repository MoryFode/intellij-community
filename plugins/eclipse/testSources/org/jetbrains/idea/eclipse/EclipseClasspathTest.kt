// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.idea.eclipse

import com.intellij.openapi.application.PluginPathManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.StdModuleTypes
import com.intellij.openapi.module.impl.ModuleManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModel
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.JDOMUtil
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.*
import com.intellij.testFramework.assertions.Assertions
import com.intellij.testFramework.rules.TempDirectory
import com.intellij.util.PathUtil
import com.intellij.util.io.div
import org.jetbrains.idea.eclipse.config.EclipseClasspathStorageProvider
import org.jetbrains.idea.eclipse.conversion.EclipseClasspathReader
import org.jetbrains.idea.eclipse.conversion.EclipseClasspathWriter
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import java.io.File

class EclipseClasspathTest {
  @JvmField
  @Rule
  val tempDirectory = TempDirectory()

  @JvmField
  @Rule
  val testName = TestName()

  @Test
  fun testAbsolutePaths() {
    doTest("parent/parent/test")
  }

  @Test
  fun testWorkspaceOnly() {
    doTest()
  }

  @Test
  fun testExportedLibs() {
    doTest()
  }

  @Test
  fun testPathVariables() {
    doTest()
  }

  @Test
  fun testJunit() {
    doTest()
  }

  @Test
  fun testSrcBinJRE() {
    doTest()
  }

  @Test
  fun testSrcBinJRESpecific() {
    doTest()
  }

  @Test
  fun testNativeLibs() {
    doTest()
  }

  @Test
  fun testAccessrulez() {
    doTest()
  }

  @Test
  fun testSrcBinJREProject() {
    doTest()
  }

  @Test
  fun testSourceFolderOutput() {
    doTest()
  }

  @Test
  fun testMultipleSourceFolders() {
    doTest()
  }

  @Test
  fun testEmptySrc() {
    doTest()
  }

  @Test
  fun testHttpJavadoc() {
    doTest()
  }

  @Test
  fun testHome() {
    doTest()
  }

  //public void testNoJava() throws Exception {
  //  doTest();

  @Test//}
  fun testNoSource() {
    doTest()
  }

  @Test
  fun testPlugin() {
    doTest()
  }

  @Test
  fun testRoot() {
    doTest()
  }

  @Test
  fun testUnknownCon() {
    doTest()
  }

  @Test
  fun testSourcesAfterAll() {
    doTest()
  }

  @Test
  fun testLinkedSrc() {
    doTest()
  }

  @Test
  fun testSrcRootsOrder() {
    doTest()
  }

  @Test
  fun testResolvedVariables() {
    doTest(setupPathVariables = true)
  }

  @Test
  fun testResolvedVars() {
    doTest("test", true, "linked")
  }

  @Test
  fun testResolvedVarsInOutput() {
    doTest("test", true, "linked")
  }

  @Test
  fun testResolvedVarsInLibImlCheck1() {
    doTest("test", true, "linked")
  }


  private fun doTest(eclipseProjectDirPath: String = "test", setupPathVariables: Boolean = false, testDataParentDir: String = "round") {
    val testDataRoot = PluginPathManager.getPluginHome("eclipse").toPath() / "testData"
    val testRoot = testDataRoot / testDataParentDir / testName.methodName.removePrefix("test").decapitalize()
    val commonRoot = testDataRoot / "common" / "testModuleWithClasspathStorage"
    val modulePath = "$eclipseProjectDirPath/${PathUtil.getFileName(eclipseProjectDirPath)}"
    checkLoadSaveRoundTrip(listOf(testRoot, commonRoot), tempDirectory, setupPathVariables, listOf("test" to modulePath))
  }

  companion object {
    @JvmField
    @ClassRule
    val appRule = ApplicationRule()

    @JvmStatic
    fun setUpModule(path: String, project: Project): Module {
      val classpathFile = File(path, EclipseXml.DOT_CLASSPATH_EXT)
      var fileText = FileUtil.loadFile(classpathFile).replace("\\\$ROOT\\$",
                                                              PlatformTestUtil.getOrCreateProjectBaseDir(project).path)
      if (!SystemInfo.isWindows) {
        fileText = fileText.replace(EclipseXml.FILE_PROTOCOL + "/", EclipseXml.FILE_PROTOCOL)
      }
      val classpathElement = JDOMUtil.load(fileText)
      val module = WriteCommandAction.runWriteCommandAction(null, (Computable {
        val imlPath = path + "/" + EclipseProjectFinder.findProjectName(path) + ModuleManagerEx.IML_EXTENSION
        ModuleManager.getInstance(project).newModule(imlPath, StdModuleTypes.JAVA.id)
      } as Computable<Module>))
      ModuleRootModificationUtil.updateModel(module) { model: ModifiableRootModel? ->
        try {
          val classpathReader = EclipseClasspathReader(path, project, null)
          classpathReader.init(model!!)
          classpathReader.readClasspath(model, classpathElement)
          EclipseClasspathStorageProvider().assertCompatible(model)
        }
        catch (e: Exception) {
          throw RuntimeException(e)
        }
      }
      return module
    }

    @JvmStatic
    fun checkModule(path: String?, module: Module) {
      val classpathFile1 = File(path, EclipseXml.DOT_CLASSPATH_EXT)
      if (!classpathFile1.exists()) return
      var fileText1 = FileUtil.loadFile(classpathFile1).replace("\\\$ROOT\\$",
                                                                PlatformTestUtil.getOrCreateProjectBaseDir(module.project).path)
      if (!SystemInfo.isWindows) {
        fileText1 = fileText1.replace(EclipseXml.FILE_PROTOCOL + "/", EclipseXml.FILE_PROTOCOL)
      }
      val classpathElement1 = JDOMUtil.load(fileText1)
      val model: ModuleRootModel = ModuleRootManager.getInstance(module)
      val resultClasspathElement = EclipseClasspathWriter().writeClasspath(classpathElement1, model)
      Assertions.assertThat(resultClasspathElement).isEqualTo(resultClasspathElement)
    }
  }
}