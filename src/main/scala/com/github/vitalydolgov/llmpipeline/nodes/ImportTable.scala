package com.github.vitalydolgov.llmpipeline.nodes

import cats.effect.*
import java.io.File
import scala.io.Source

object ImportTable extends OpNode[String] {
  def execute(input: String): IO[String] = IO.blocking {
    val file = File(input)
    if (!file.exists() || !file.isFile) {
      throw new Exception(s"CSV file not found: $input")
    }

    val lines = Source.fromFile(file).getLines().toList
    if (lines.isEmpty) {
      throw new Exception(s"CSV file is empty: $input")
    }

    val headers = parseCsvLine(lines.head)
    val rows = lines.tail.map(parseCsvLine)

    toMarkdownTable(headers, rows)
  }

  private def parseCsvLine(line: String): List[String] =
    line.split(",").map(_.trim).toList

  private def toMarkdownTable(headers: List[String], rows: List[List[String]]): String = {
    val headerRow = "| " + headers.mkString(" | ") + " |"
    val separator = "| " + headers.map(_ => "---").mkString(" | ") + " |"
    val dataRows = rows.map(row => "| " + row.mkString(" | ") + " |")
    (headerRow :: separator :: dataRows).mkString("\n")
  }
}
