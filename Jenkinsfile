pipeline {
  agent any
  // {
  //   // Run on a build agent where we have the Android SDK installed
  //   label any
  // }
  environment {
    ANDROID_SDK_ROOT = '/Users/robinshi/Library/Android/sdk'

    BUILD_LIB_DOWNLOAD_FOLDER = '${WORKSPACE}/mega_build_download'
    WEBRTC_LIB_URL = "https://mega.nz/file/t81HSYJI#KQNzSEqmGVSXfwmQx2HMJy3Jo2AcDfYm4oiMP_CFW6s"
    WEBRTC_LIB_FILE = 'WebRTC_NDKr16b_m76_p21.zip'
    WEBRTC_LIB_UNZIPPED = 'webrtc_unzipped'
    GOOGLE_MAP_API_URL = "https://mega.nz/#!1tcl3CrL!i23zkmx7ibnYy34HQdsOOFAPOqQuTo1-2iZ5qFlU7-k"
    GOOGLE_MAP_API_FILE = 'default_google_maps_api.zip'
    GOOGLE_MAP_API_UNZIPPED = 'default_google_map_api_unzipped'
  }
  options {
    // Stop the build early in case of compile or test failures
    skipStagesAfterUnstable()
  }
  stages {
    stage('fetch SDK') {
      steps {
        sh 'git config --file=.gitmodules submodule."app/src/main/jni/mega/sdk".url https://code.developers.mega.co.nz/sdk/sdk.git'
        sh 'git config --file=.gitmodules submodule."app/src/main/jni/mega/sdk".branch develop'
        sh 'git config --file=.gitmodules submodule."app/src/main/jni/megachat/sdk".url https://code.developers.mega.co.nz/megachat/MEGAchat.git'
        sh 'git config --file=.gitmodules submodule."app/src/main/jni/megachat/sdk".branch develop'
        sh 'git submodule sync'
        sh 'git submodule update --init --recursive --remote'
      }
    }
    stage('Download Dependency Lib for SDK') {
      steps {
        sh """
        export PATH=/Applications/MEGAcmd.app/Contents/MacOS:$PATH
        mkdir -p "${BUILD_LIB_DOWNLOAD_FOLDER}"
        cd "${BUILD_LIB_DOWNLOAD_FOLDER}"
        pwd
        ls -lh

        ## check webrtc file
        if test -f "${BUILD_LIB_DOWNLOAD_FOLDER}/${WEBRTC_LIB_FILE}"; then
          echo "${WEBRTC_LIB_FILE} already downloaded. Skip downloading."
        else
          echo "downloading webrtc"
          mega-get ${WEBRTC_LIB_URL}
        fi
        if [ -d "${WEBRTC_LIB_UNZIPPED}" ]; then
          echo "webrtc already unzipped"
        else
          unzip ${WEBRTC_LIB_FILE} -d ${WEBRTC_LIB_UNZIPPED}
        fi

        ## check default google api
        if test -f "${BUILD_LIB_DOWNLOAD_FOLDER}/${GOOGLE_MAP_API_FILE}"; then
          echo "${GOOGLE_MAP_API_FILE} already downloaded. Skip downloading."
        else
          echo "downloading google map api"
          mega-get ${GOOGLE_MAP_API_URL}
        fi
        if [ -d "${GOOGLE_MAP_API_UNZIPPED}" ]; then
          echo "default_google_map_api already unzipped"
        else
          unzip ${GOOGLE_MAP_API_FILE} -d ${GOOGLE_MAP_API_UNZIPPED}
        fi

        ls -lh

        cd ${WORKSPACE}
        pwd
        # apply dependency patch
        cp -fr ${BUILD_LIB_DOWNLOAD_FOLDER}/${WEBRTC_LIB_UNZIPPED}/webrtc app/src/main/jni/megachat/
        # mkdir -p app/src
        cp -fr ${BUILD_LIB_DOWNLOAD_FOLDER}/${GOOGLE_MAP_API_UNZIPPED} app/src

        """
      }
    }
    stage('Build SDK') {
      steps {
        sh """
        export PATH=/usr/local/opt/gnu-sed/libexec/gnubin:/usr/local/opt/gnu-tar/libexec/gnubin:/usr/local/opt/openjdk@11/bin:/usr/local/bin:$PATH
        cd ${WORKSPACE}/app/src/main/jni
        /usr/local/bin/bash build.sh all
        """
      }
    }
    stage('Compile') {
      steps {
        // Compile the app and its dependencies
        sh './gradlew compileDebugSources'
      }
    }
    stage('Unit test') {
      steps {
        // Compile and run the unit tests for the app and its dependencies
        sh './gradlew testDebugUnitTest'

        // Analyse the test results and update the build result as appropriate
        //junit '**/TEST-*.xml'
      }
    }
    stage('Build APK') {
      steps {
        // Finish building and packaging the APK
        sh './gradlew assembleDebug'

        // Archive the APKs so that they can be downloaded from Jenkins
        archiveArtifacts '**/*.apk'
      }
    }
    // stage('Static analysis') {
    //   steps {
    //     // Run Lint and analyse the results
    //     sh './gradlew lintDebug'
    //     androidLint pattern: '**/lint-results-*.xml'
    //   }
    // }
    stage('Deploy') {
      when {
        // Only execute this stage when building from the `beta` branch
        branch 'beta'
      }
      environment {
        // Assuming a file credential has been added to Jenkins, with the ID 'my-app-signing-keystore',
        // this will export an environment variable during the build, pointing to the absolute path of
        // the stored Android keystore file.  When the build ends, the temporarily file will be removed.
        SIGNING_KEYSTORE = credentials('my-app-signing-keystore')

        // Similarly, the value of this variable will be a password stored by the Credentials Plugin
        SIGNING_KEY_PASSWORD = credentials('my-app-signing-password')
      }
      steps {
        // Build the app in release mode, and sign the APK using the environment variables
        sh './gradlew assembleRelease'

        // Archive the APKs so that they can be downloaded from Jenkins
        archiveArtifacts '**/*.apk'

        // Upload the APK to Google Play
        androidApkUpload googleCredentialsId: 'Google Play', apkFilesPattern: '**/*-release.apk', trackName: 'beta'
      }
      // post {
      //   success {
      //     // Notify if the upload succeeded
      //     mail to: 'beta-testers@example.com', subject: 'New build available!', body: 'Check it out!'
      //   }
      // }
    }
  }
  // post {
  //   failure {
  //     // Notify developer team of the failure
  //     mail to: 'android-devs@example.com', subject: 'Oops!', body: "Build ${env.BUILD_NUMBER} failed; ${env.BUILD_URL}"
  //   }
  // }
}