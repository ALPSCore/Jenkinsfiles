def call(String compiler, String mpilib) {
    def script = '''
module() { echo "Called module $@"; }
module purge
module add cmake
# EIGEN3_INCLUDE_DIR=$HOME/.local/packages/eigen-3.3.4
EIGEN3_INCLUDE_DIR=/usr/local/eigen/eigen_3.3.4
'''

    switch (mpilib) {
        case 'MPI_OFF':
            script += 'ENABLE_MPI=OFF'
            break
        case 'OpenMPI':
            script += 'ENABLE_MPI=ON; module add openmpi/1.10.7'
            break
        default:
            error "Do not know how to handle MPI library ${mpilib}"
    }

    script += "\n"

    switch (compiler) {
        case "gcc_5.4.0":
            script += 'module add gnu/5.4.0; '
            // NOTE: fall through!
        case "gcc_4.8.5":
            script += 'export CC=$(which gcc); export CXX=$(which g++); '
            break
        case "clang_5.0.1":
            script += 'module add llvm5/5.0.1; '
            // NOTE: fall through!
        case "clang_3.4.2":
            script += 'export CC=$(which clang); export CXX=$(which clang++); '
            break
        case "intel_18.0.5":
            script += '''. /opt/intel/bin/compilervars.sh intel64
export CC=/opt/intel/bin/icc
export CXX=/opt/intel/bin/icpc
'''
        default:
            error "Do not know how to handle compiler ${compiler}"
    }
    return script
}
