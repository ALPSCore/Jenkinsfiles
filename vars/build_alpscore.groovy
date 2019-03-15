/* Build ALPSCore */

// @Library('alpscore_jenkins_pipeline') _

def make_stage(String title, String comp, String lib) {
    def dirname = "compiler=${comp}_mpilib=${lib}"
    def archive = "alpscore_${dirname}.zip"

    return { ->
        stage ("Building ${title}") {
            def shell_script=shell_setup_environment(comp, lib)
            stash dirname // FIXME: it may be better to checkout on the node instead
            node {
                unstash dirname // FIXME: it may be better to checkout here instead
                dir (dirname) {
                    stage ("Clean working directory") {
                        sh 'echo rm -rf *'
                    }
                    stage ("Configure") {
                        sh """${shell_script}
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
make -j4
"""
                    }
                    stage ("Test") {
                        sh """${shell_script}
make test
"""
                        junit "*/test/*.xml"
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

def run_build_steps(String compilers_str, String mpilibs_str, String skips_str) {
    def compilers = compilers_str.tokenize(" ");
    def mpilibs = mpilibs_str.tokenize(" ");
    def skips = skips_str.tokenize(" ");
    for (comp in compilers) {
        for (lib in mpilibs) {
            if ((comp+':'+lib) in skips) { continue }
            def this_stage = make_stage("${comp} ${lib}", comp, lib)
            this_stage() // or save it and run in parallel later!
        }
    }
}

def call(Map args) {
    def compilers = args.compilers;
    def mpilibs = args.mpilibs;
    // FIXME: this should be in a separate configuration file:
    def skips = args.skip?:'gcc_4.8.5:OpenMPI clang_3.4.2:OpenMPI clang_5.0.1:OpenMPI intel_18.0.5:OpenMPI';
    pipeline {
        agent any
        parameters {
            string(name: 'COMPILERS',
                   description: 'Compilers to use',
                   defaultValue: compilers)
            string(name: 'MPILIBS',
                   description: 'MPI libraries to use',
                   defaultValue: mpilibs)
            string(name: 'SKIP',
                   description: 'Compiler:MPI pairs to skip',
                   defaultValue: skips
            )
        }
        stages {
            stage ('Multistage') {
                steps {
                    script {
                        run_build_steps(params.COMPILERS, params.MPILIBS, params.SKIP)
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

