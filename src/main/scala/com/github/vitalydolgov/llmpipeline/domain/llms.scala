package com.github.vitalydolgov.llmpipeline.domain

object llms {
  enum LLM(val name: String) {
    case Sonnet45 extends LLM("claude-sonnet-4-5")
    case Haiku35 extends LLM("claude-3-5-haiku-latest")
  }

  enum Temperature(val value: Double) {
    case Precise extends Temperature(0.2)
    case Balanced extends Temperature(0.4)
    case Creative extends Temperature(0.6)
    case Exploratory extends Temperature(0.8)
  }
}
