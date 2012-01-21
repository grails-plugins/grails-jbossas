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
import groovy.xml.DOMBuilder
import groovy.xml.XmlUtil
import groovy.xml.dom.DOMCategory

eventCreateWarStart = { warName, stagingDir ->
	try {
		def jbossConfig = grailsApp.config.grails.plugin.jbossas

		boolean removeLog4jWebxml = getConfigBoolean(jbossConfig, 'removeLog4jWebxml', true)
		if (removeLog4jWebxml) {
			removeLog4jFromWebXml stagingDir
		}

		boolean removeJars = getConfigBoolean(jbossConfig, 'removeLog4jJars', true)
		if (removeJars) {
			removeLog4jJars stagingDir
		}
	}
	catch (e) {
		GrailsUtil.deepSanitize e
		e.printStackTrace()
	}
}

private void removeLog4jFromWebXml(stagingDir) {
	def webXmlFile = new File(stagingDir, '/WEB-INF/web.xml')
	def wxml = DOMBuilder.parse(new StringReader(webXmlFile.text)).documentElement

	String className = 'org.codehaus.groovy.grails.plugins.log4j.web.util.Log4jConfigListener'
	use (DOMCategory) {
		def listenerNodes = wxml.'listener'
		for (n in listenerNodes) {
			if (n.'listener-class'.text() == className) {
				wxml.removeChild n
			}
		}
	}

	webXmlFile.withWriter { it << XmlUtil.serialize(wxml) }
}

private void removeLog4jJars(stagingDir) {
	def jarPatterns = grailsApp.config.grails.plugin.jbossas.deleteJarPatterns
	if (!(jarPatterns instanceof Collection)) {
		jarPatterns = ['log4j*', 'slf4j*', 'jcl-over-slf4j*', 'jul-to-slf4j*']
	}
	for (pattern in jarPatterns) {
		ant.delete(verbose: true) {
			fileset dir: "$stagingDir/WEB-INF/lib/",
			        includes: "${pattern}.jar"
		}
	}
}

private Boolean getConfigBoolean(config, String name, Boolean defaultIfNotSet) {
	def value = config[name]
	return value instanceof Boolean ? value : defaultIfNotSet
}
