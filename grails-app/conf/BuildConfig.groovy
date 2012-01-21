import grails.util.Metadata

grails.project.work.dir = 'target'
grails.project.docs.output.dir = 'docs/manual' // for backwards-compatibility, the docs are checked into gh-pages branch

grails.project.dependency.resolution = {

	inherits 'global'
	log 'warn'

	repositories {
		grailsCentral()
	}

	plugins {
		if (Metadata.current.getGrailsVersion()[0] != '1') {
			build(':release:1.0.0') { export = false }
		}
	}
}
