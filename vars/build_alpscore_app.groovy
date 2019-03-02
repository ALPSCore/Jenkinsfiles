/* Build an app that needs ALPSCore */

def get_label(String comp, String lib) {
    "compiler=${comp}_mpilib=${lib}"
}

def make_stage(String title, String comp, String lib, Map alpscore_loc) {
    def dirname = get_label(comp, lib)
    def archive = "${dirname}_installed.zip"
    // These variables must be made local to bind to the closure:
    def alpscore_artifact = alpscore_loc.artifact
    def alpscore_project = alpscore_loc.project
    return { ->
        stage ("Building ${title}") {
            def shell_script=shell_setup_environment(comp, lib)
            stash dirname // FIXME: it may be better to checkout on the node instead
            node {
                unstash dirname // FIXME: it may be better to checkout here instead
                dir (dirname) {
                    stage ("Clean working directory") {
                        sh 'rm -rf *'
                    }
                    stage ("Obtain ALPSCore artifact") {
                        copyArtifacts(projectName: alpscore_project,
                                      selector: lastSuccessful(),
                                      filter: alpscore_artifact,
                                      fingerprintArtifacts: true,
                                      optional: false)

                        sh "unzip ${alpscore_artifact} -d alpscore/"
                    }
                    stage ("Configure") {
                        // FIXME:make shell scripts customizable
                        sh """${shell_script}
export ALPSCore_DIR=\$PWD/alpscore
cmake .. -DCMAKE_BUILD_TYPE=Debug -DCMAKE_INSTALL_PREFIX=\$PWD/installed -DTestXMLOutput=ON
"""
                    }
                    stage ("Build") {
                        sh """${shell_script}
make -j4
"""
                    }
                    stage ("Test") {
                        sh """${shell_script}
make test
"""
                        junit "test/*.xml" // FIXME:make test location customizable
                    }
                    stage ("Install") {
                        sh """${shell_script}
rm -rf ${archive}
make -j4 install
"""
                        zip(archive: true,
                            dir: "installed",
                            glob: '',
                            zipFile: archive)
                        fingerprint archive 
                    }
                } // end dirname
            } // end node
        } // end stage

    } // end closure
}

def run_build_steps(String compilers_str, String mpilibs_str, String alpscore_project) {
    def compilers = compilers_str.tokenize(" ");
    def mpilibs = mpilibs_str.tokenize(" ");
    def alpscore_loc = [ project: alpscore_project ]
    def par_jobs = [:]
    for (comp in compilers) {
        for (lib in mpilibs) {
            alpscore_loc.artifact = "alpscore_${get_label(comp, lib)}.zip"
            def this_stage = make_stage("${comp} ${lib}", comp, lib, alpscore_loc)
            // Sequential run:
            // this_stage()
            // Save for parallel run:
            par_jobs[get_label(comp,lib)]=this_stage;
        }
    }
    // Run saved jobs in parallel
    parallel(par_jobs)
}

def call(Map args) {
    def compilers = args.compilers;
    def mpilibs = args.mpilibs;
    pipeline {
        agent any
        parameters {
            string(name: 'COMPILERS',
                   description: 'Compilers to use',
                   defaultValue: compilers)
            string(name: 'MPILIBS',
                   description: 'MPI libraries to use',
                   defaultValue: mpilibs)
            string(name: 'ALPSCORE_PROJECT',
                   description: 'Project to copy ALPSCore installation from',
                   defaultValue: 'ALPSCore_local/issue.jenkins_pipeline_library')
            
        }
        stages {
            stage ('Multistage') {
                steps {
                    run_build_steps(params.COMPILERS,
                                    params.MPILIBS,
                                    params.ALPSCORE_PROJECT)
                }
            }
        }
        
        post {
            always {
                echo 'DEBUG: Build is over'
            }
            success {
                echo 'DEBUG: Build successful'
            }
            unstable {
                echo 'DEBUG: Build is unstable'
                // emailext to: 'galexv+jenkins.status@umich.edu',
                // recipientProviders: [brokenTestsSuspects(), culprits(), requestor()],
                // subject: 'ALPSCore: Jenkins build is unstable',
                // attachLog: true,
                // compressLog: true,
                // body: "ALPSCore build is unstable: see attached log"
            }
            failure {
                echo 'DEBUG: Build failed'
                // emailext to: 'galexv+jenkins.status@umich.edu',
                // recipientProviders: [brokenTestsSuspects(), culprits(), requestor()],
                // subject: 'ALPSCore: Jenkins build has failed',
                // attachLog: true,
                // compressLog: true,
                // body: "ALPSCore build has failed: see attached log"
            }
            changed {
                echo 'DEBUG: Build status changed'
                // emailext to: 'galexv+jenkins.status@umich.edu',
                // recipientProviders: [brokenTestsSuspects(), culprits(), requestor()],
                // subject: 'ALPSCore: Jenkins build status changed',
                // attachLog: true,
                // compressLog: true,
                // body: "ALPSCore build status changed"
            }
        }
        
    }
}
