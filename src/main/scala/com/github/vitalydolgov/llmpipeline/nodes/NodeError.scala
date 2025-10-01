package com.github.vitalydolgov.llmpipeline.nodes

sealed trait NodeError
case class NoDataError(message: String) extends NodeError
