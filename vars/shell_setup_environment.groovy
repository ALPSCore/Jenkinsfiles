def call(String compiler, String mpilib, List extra_libs) {
    def script = siteConfig.initShell;

    script += siteConfig.compilers[compiler]?:error("Do not know how to handle compiler ${compiler}");
    script += "\n";
    script += siteConfig.mpiLibs[mpilib]?:error("Do not know how to handle MPI library ${mpilib}");

    for (lib in extra_libs) {
        script += "\n";
        script += siteConfig.extraLibs[lib]?:error("Do not know how to handle library ${lib}");
    }
    return script;
}
