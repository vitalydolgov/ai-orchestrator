package com.github.vitalydolgov.llmpipeline.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

final case class AppConfig(anthropicApiKey: String, inputData: String, outputArtifacts: String)
    derives ConfigReader
