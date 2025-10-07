package com.github.vitalydolgov.llmpipeline.nodes

import cats.effect.IO
import com.anthropic.client.AnthropicClient
import scala.io.Source
import java.nio.file.*
import java.nio.charset.StandardCharsets
import java.io.File

trait InjectHtml { self: LlmNode =>
  def injectData(
      templatePath: String,
      data: String,
      outputPath: String
  )(using client: AnthropicClient): IO[Unit] = {
    for {
      // Read the template file
      template <- IO.blocking(Source.fromFile(templatePath).mkString)

      // Ask LLM to inject data into the template
      prompt =
        s"""Given the following HTML template and data, inject the data into the template with the appropriate information.
        |
        |HTML Template:
        |$template
        |
        |Data to inject:
        |$data
        |
        |When injecting the data:
        |- Format lists of statements, items, or multiple related points as HTML bullet points (<ul><li>...</li></ul>)
        |- Use bullet points for any enumerated items, features, or lists in the data
        |- Maintain proper HTML structure and formatting
        |
        |Please return only the HTML with injected data without any explanation or markdown formatting.""".stripMargin

      injectedHtml <- ask(prompt)

      // Write the HTML with injected data to output file
      _ <- IO.blocking {
        Files.write(
          Paths.get(outputPath),
          injectedHtml.getBytes(StandardCharsets.UTF_8),
          StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING
        )
      }

      // Copy assets directory from template location to output location
      _ <- copyAssets(templatePath, outputPath)
    } yield ()
  }

  private def copyAssets(templatePath: String, outputPath: String): IO[Unit] = IO.blocking {
    val templateDir = new File(templatePath).getParentFile
    val outputDir = new File(outputPath).getParentFile
    val assetsSource = new File(templateDir, "assets")
    val assetsDest = new File(outputDir, "assets")

    if (assetsSource.exists() && assetsSource.isDirectory) {
      copyDirectory(assetsSource, assetsDest)
    }
  }

  private def copyDirectory(source: File, dest: File): Unit = {
    if (!dest.exists()) {
      dest.mkdirs()
    }

    source.listFiles().foreach { file =>
      val destFile = new File(dest, file.getName)
      if (file.isDirectory) {
        copyDirectory(file, destFile)
      } else {
        try {
          Files.copy(file.toPath, destFile.toPath, StandardCopyOption.REPLACE_EXISTING)
        } catch {
          case _: FileAlreadyExistsException => // Do nothing
        }
      }
    }
  }
}
