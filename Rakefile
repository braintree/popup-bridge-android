require 'rake'
require 'io/console'

TMP_CHANGELOG_FILE = "/tmp/popup-bridge-release.md"

task :default => :unit_tests

desc "Run Android lint on all modules"
task :lint do
  sh "./gradlew clean lint"
end

desc "Run Android unit tests"
task :unit_tests => :lint do
  sh "./gradlew --continue test"
end

desc "Run Android tests on a device or emulator"
task :integration_tests do
  output = `adb devices`
  if output.match(/device$/)
      sh "./gradlew --continue connectedAndroidTest"
  else
    puts "Please connect a device or start an emulator and try again"
    exit 1
  end
end

desc "Interactive release to publish new version"
task :release => :unit_tests do
  Rake::Task["assumptions"].invoke

  puts "What version are you releasing? (x.x.x format)"
  version = $stdin.gets.chomp

  update_version(version)
  update_readme_version(version)

  prompt_for_sonatype_username_and_password

  sh "./gradlew clean :PopupBridge:publishToSonatype"
  sh "./gradlew closeAndReleaseRepository"

  post_release(version)
end

task :assumptions do
    puts "Release Assumptions"
    puts "* [ ] You are on the branch and commit you want to release."
    puts "* [ ] You have already merged hotfixes and pulled changes."
    puts "* [ ] You have already reviewed the diff between the current release and the last tag, noting breaking changes in the semver and CHANGELOG."

    puts "Ready to release? Press any key to continue. "
    $stdin.gets
end

desc "Interactive release to publish new beta version"
task :release_beta => :unit_tests do
  Rake::Task["assumptions"].invoke

  puts "What version are you releasing? (x.x.x format)"
  version = $stdin.gets.chomp

  update_version(version)
  update_migration_guide_version(version)

  prompt_for_sonatype_username_and_password

  sh "./gradlew clean :PopupBridge:publishToSonatype"
  sh "./gradlew closeAndReleaseRepository"

  puts "\nArchives are uploaded! Committing and tagging #{version} and preparing for the next development iteration"

  sh "git commit -am 'Release #{version}'"
  sh "git tag #{version} -a -m 'Release #{version}'"

  version_match = version.match /(\d+\.\d+\.\d+-beta)(\d+)/
  beta_version_prefix = version_match[1]
  next_beta_version_number = version_match[2].to_i + 1

  update_version("#{beta_version_prefix}#{next_beta_version_number}-SNAPSHOT")
  increment_version_code
  sh "git commit -am 'Prepare for deployment'"

  puts "\nDone. Commits and tags have been created. If everything appears to be in order, hit ENTER to push."
  $stdin.gets

  sh "git push origin main #{version}"
end

def prompt_for_sonatype_username_and_password
  puts "Enter Sonatype username:"
  ENV["SONATYPE_USERNAME"] = $stdin.gets.chomp

  puts "Enter Sonatype password:"
  ENV["SONATYPE_PASSWORD"] = $stdin.noecho(&:gets).chomp
end

def post_release(version)
  puts "\nArchives are uploaded! Committing and tagging #{version} and preparing for the next development iteration"

  sh "git commit -am 'Release #{version}'"
  sh "git tag #{version} -a -m 'Release #{version}'"

  version_values = version.split('.')
  version_values[2] = version_values[2].to_i + 1
  update_version("#{version_values.join('.')}-SNAPSHOT")
  update_readme_snapshot_version(version_values.join('.'))
  increment_version_code
  sh "git commit -am 'Prepare for deployment'"

  puts "\nDone. Commits and tags have been created. If everything appears to be in order, hit ENTER to push."
  $stdin.gets

  sh "git push origin main #{version}"
end

def get_current_version
  current_version = nil
  File.foreach("build.gradle") do |line|
    if match = line.match(/^version '(\d+\.\d+\.\d+(-SNAPSHOT)?)'/)
      current_version = match.captures
    end
  end

  return current_version[0]
end

def increment_version_code
  new_build_file = ""
  File.foreach("build.gradle") do |line|
    if line.match(/versionCode = (\d+)/)
      new_build_file += line.gsub(/versionCode = \d+/, "versionCode = #{$1.to_i + 1}")
    else
      new_build_file += line
    end
  end
  IO.write('build.gradle', new_build_file)
end

def update_version(version)
  IO.write("build.gradle",
    File.open("build.gradle") do |file|
      file.read.gsub(/^version '\d+\.\d+\.\d+(-beta\d+)?(-SNAPSHOT)?'/, "version '#{version}'")
    end
  )
end

def update_readme_version(version)
  IO.write("README.md",
    File.open("README.md") do |file|
      file.read.gsub(/:popup-bridge:\d+\.\d+\.\d+'/, ":popup-bridge:#{version}'")
    end
  )
end

def update_readme_snapshot_version(snapshot_version)
  IO.write("README.md",
    File.open("README.md") do |file|
      file.read.gsub(/:popup-bridge:\d+\.\d+\.\d+-SNAPSHOT'/, ":popup-bridge:#{snapshot_version}-SNAPSHOT'")
    end
  )
end

def update_migration_guide_version(version)
  IO.write("v4_MIGRATION.md",
    File.open("v4_MIGRATION.md") do |file|
      file.read.gsub(/:popup-bridge:\d+\.\d+\.\d+-.*'/, ":popup-bridge:#{version}'")
    end
  )
end

