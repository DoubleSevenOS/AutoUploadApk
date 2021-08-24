package com.yyhu.auto

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class MyTask extends DefaultTask {
    String input = '***********************************Start Auto Build*******************************************'

    @TaskAction
    def greet() {
        println input
    }
}
