// Site configuration

// Base configuration shell script
@groovy.transform.Field
def initShell = '''
module() { echo "Called module $@"; }
module purge
module add cmake
export BOOST_ROOT=$(module purge; module add gnu openmpi boost; echo $BOOST_ROOT)
# EIGEN3_INCLUDE_DIR=$HOME/.local/packages/eigen-3.3.4
EIGEN3_INCLUDE_DIR=/usr/local/eigen/eigen_3.3.4
make_jobs=4
'''

// Mapping compiler --> shell script snippet to activate
@groovy.transform.Field
def compilers = [
    'gcc_5.4.0' : 'module add gnu/5.4.0; export CC=$(which gcc); export CXX=$(which g++)',
    'gcc_4.8.5' : 'export CC=$(which gcc); export CXX=$(which g++)',
    'clang_5.0.1' : 'module add llvm5/5.0.1; export CC=$(which clang); export CXX=$(which clang++)',
    'clang_3.4.2' : 'export CC=$(which clang); export CXX=$(which clang++)',
    'intel_18.0.5' : 'module add Intel/2018.5.274; export CC=$(which icc); export CXX=$(which icpc)',
];

// Mapping MPI library --> shell script snippet to activate
@groovy.transform.Field
def mpiLibs = [
    'MPI_OFF' : 'ENABLE_MPI=OFF',
    'OpenMPI' : 'ENABLE_MPI=ON; module add openmpi/1.10.7',
];

// Mapping extra libs --> shell script snippets to activate
@groovy.transform.Field
def extraLibs = [
    'gsl' : 'export GSL_ROOT_DIR=$(module purge; module add gnu gsl; echo $GSL_DIR)',
];
