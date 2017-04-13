package com.dx168.fastdex.build.transform

import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.dx168.fastdex.build.util.ClassInject
import com.dx168.fastdex.build.util.FastdexUtils
import com.dx168.fastdex.build.util.JarOperation
import com.dx168.fastdex.build.variant.FastdexVariant
import com.android.build.api.transform.Format
import com.dx168.fastdex.build.util.FileUtils

/**
 * 拦截transformClassesWithJarMergingFor${variantName}任务,
 * Created by tong on 17/27/3.
 */
class FastdexJarMergingTransform extends TransformProxy {
    FastdexVariant fastdexVariant

    FastdexJarMergingTransform(Transform base, FastdexVariant fastdexVariant) {
        super(base)
        this.fastdexVariant = fastdexVariant
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, IOException, InterruptedException {
        if (fastdexVariant.hasDexCache) {
            if (fastdexVariant.projectSnapshoot.diffResultSet.isJavaFileChanged()) {
                //补丁jar
                File patchJar = getCombinedJarFile(transformInvocation)
                //所有的class目录
                Set<File> directoryInputFiles = FastdexUtils.getDirectoryInputFiles(transformInvocation)
                //生成补丁jar
                JarOperation.generatePatchJar(fastdexVariant,directoryInputFiles,patchJar)
            }
            else {
                fastdexVariant.project.logger.error("==fastdex no java files have changed, just ignore")
            }
        }
        else {
//            Set<File> dirClasspaths = new HashSet<>();
//            for (TransformInput input : transformInvocation.getInputs()) {
//                Collection<JarInput> directoryInputs = input.getJarInputs()
//                if (directoryInputs != null) {
//                    for (JarInput directoryInput : directoryInputs) {
//                        dirClasspaths.add(directoryInput.getFile())
//                    }
//                }
//            }
//            for (File f : dirClasspaths) {
//                fastdexVariant.project.logger.error("==fastdex jarInput: ${f}")
//            }


            //inject dir input
            Set<File> directoryInputFiles = FastdexUtils.getDirectoryInputFiles(transformInvocation)
            ClassInject.injectDirectoryInputFiles(fastdexVariant,directoryInputFiles)
            base.transform(transformInvocation)
        }
    }

    /**
     * 获取输出jar路径
     * @param invocation
     * @return
     */
    public File getCombinedJarFile(TransformInvocation invocation) {
        def outputProvider = invocation.getOutputProvider();

        // all the output will be the same since the transform type is COMBINED.
        // and format is SINGLE_JAR so output is a jar
        File jarFile = outputProvider.getContentLocation("combined", base.getOutputTypes(), base.getScopes(), Format.JAR);
        FileUtils.ensumeDir(jarFile.getParentFile());
        return jarFile
    }
}