/* Build ALPSCore */

// @Library('alpscore_jenkins_pipeline') _

def make_stage(String title, String comp, String lib) {
    def dirname = "compiler=${comp}_mpilib=${lib}"

    { ->
        stage ("Building ${title}") {
            def shell_script=shell_setup_environment(comp, lib)
            node {
                dir (dirname) {
                    stage ("Clean working directory") {
                        sh 'rm -rf *'
                    }
                    stage ("Configure") {
                        sh """${shell_script}
mkdir build && cd build
cmake .. \
      -DCMAKE_INSTALL_PREFIX=\$PWD/installed \
      -DTesting=ON -DExtensiveTesting=OFF \
      -DCMAKE_BUILD_TYPE=Release \
      -DENABLE_MPI=\${ENABLE_MPI} \
      -DTestXMLOutput=TRUE \
      -DEIGEN3_INCLUDE_DIR=\${EIGEN3_INCLUDE_DIR} \
      -DDocumentation=OFF \
"""
                    }
                    stage ("Build") {
                        sh """${shell_script}
cd build
make -j4
"""
                    }
                    stage ("Test") {
                        sh """${shell_script}
cd build
make test
"""
                        junit "build/*/test/*.xml"
                    }
                    stage ("Install") {
                        sh """${shell_script}
cd build
make -j4 install
"""
                        zip(archive: true,
                            dir: "build/installed",
                            glob: '',
                            zipFile: "alpscore_${dirname}.zip")
                        fingerprint "alpscore_${dirname}.zip"
                    }
                } // end dirname
            } // end node
        } // end stage

    } // end closure
}

def run_build_steps(String compilers_str, String mpilibs_str) {
    def compilers = compilers_str.tokenize(" ");
    def mpilibs = mpilibs_str.tokenize(" ");
    for (comp in compilers) {
        for (lib in mpilibs) {
            def this_stage = make_stage("${comp} ${lib}", comp, lib)
            this_stage() // or save it and run in parallel later!
        }
    }
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
        }
        stages {
            stage ('Multistage') {
                steps {
                    script {
                        run_build_steps(params.COMPILERS, params.MPILIBS)
                    }
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

