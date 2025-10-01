package com.github.vitalydolgov.llmpipeline.dsl

extension (sc: StringContext)
  def prompt(args: Any*): String =
    val parts = sc.parts.iterator
    val argsIter = args.iterator
    val result = new StringBuilder(parts.next())

    while argsIter.hasNext do
      result.append(argsIter.next())
      result.append(parts.next())

    val lines = result.toString.split("\n")
    if lines.isEmpty then ""
    else
      val nonEmptyLines = lines.filter(_.trim.nonEmpty)
      if nonEmptyLines.isEmpty then ""
      else
        val minIndent = nonEmptyLines.map(_.takeWhile(_.isWhitespace).length).min
        lines
          .map(line =>
            if line.trim.isEmpty then ""
            else "|" + line.drop(minIndent)
          )
          .mkString("\n")
          .stripMargin
