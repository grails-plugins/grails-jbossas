/* Copyright 2012 SpringSource.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import grails.util.GrailsUtil
import groovy.text.SimpleTemplateEngine

USAGE = """
Usage: grails generate-jboss-deploy <version>

Generate deployment files for the specified version of JBoss.
The version is a whole number and must be one of 5 or 6

Example: grails generate-jboss-deploy 5
Example: grails generate-jboss-deploy 6
"""

includeTargets << grailsScript('_GrailsBootstrap')

templateDir = "$jbossasPluginDir/src/templates"
templateEngine = new SimpleTemplateEngine()

target(generateJbossDeploy: 'Create JBoss deployment files') {
	depends(checkVersion, configureProxy, compile, createConfig)

	Integer version = determineVersion()
	if (!version) {
		errorMessage "Version or not specified or invalid; must be 5 or 6\n"
		return
	}

	try {
		if (version == 5) {
			createJBoss5Files()
		}
		else if (version == 6) {
			createJBoss6Files()
		}
//		else if (version == 7) {
//			createJBoss7Files()
//		}
	}
	catch (e) {
		GrailsUtil.deepSanitize e
		e.printStackTrace()
	}
}

private Integer determineVersion() {
	def versionArg = argsMap.params[0]
	Integer version
	if (versionArg) {
		try {
			version = versionArg as Integer
			if (version < 5 || version > 6) {
				version = null
			}
		}
		catch (NumberFormatException ignored) {}
	}
	version
}

private void createJBoss5Files() {
	String templatePath = findTemplatePath('jboss-web-5.xml')
	if (!templatePath) return
	createContainerConfigFile 'jboss-web.xml', templatePath,
		[appname: grailsAppName,
		 archive: grailsAppName + '.war',
		 java2ParentDelegation: false,
		 java2ClassLoadingCompliance: false]
}

private void createJBoss6Files() {
	String templatePath = findTemplatePath('jboss-classloading-6.xml')
	if (!templatePath) return
	createContainerConfigFile 'jboss-classloading.xml', templatePath,
		[domain: grailsAppName + '.war']

	templatePath = findTemplatePath('jboss-scanning-6.xml')
	if (!templatePath) return
	createContainerConfigFile 'jboss-scanning.xml', templatePath, [:]
}

//private void createJBoss7Files() {
//}

private void createContainerConfigFile(String path, String templatePath, Map templateAttributes) {

	File file = new File(basedir, 'web-app/WEB-INF/' + path)
	if (file.exists()) {
		printMessage "\nNot overwriting existing $file.path"
		return
	}

	generateFile templatePath, file.path, templateAttributes
}

private void generateFile(String templatePath, String outputPath, Map templateAttributes) {

	File templateFile = new File(templatePath)
	if (!templateFile.exists()) {
		errorMessage "$templatePath doesn't exist"
		return
	}

	File outFile = new File(outputPath)

	// in case it's in a package, create dirs
	ant.mkdir dir: outFile.parentFile

	outFile.withWriter { writer ->
		templateEngine.createTemplate(templateFile.text).make(templateAttributes).writeTo(writer)
	}

	printMessage "generated $outFile.absolutePath"
}

private String findTemplatePath(String key) {
	def configPaths = config.grails.plugin.jbossas.templates
	String path = templateDir + '/' + key
	if (configPaths instanceof Map) {
		def configPath = configPaths[key]
		if (configPath && configPath instanceof CharSequence) {
			if (!new File(configPath).exists()) {
				errorMessage "Template $configPath doesn't exist\n"
				return null
			}
			path = configPath
		}
	}
	path
}

printMessage = { String message -> event('StatusUpdate', [message]) }
errorMessage = { String message -> event('StatusError',  [message]) }

setDefaultTarget 'generateJbossDeploy'
