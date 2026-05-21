default:
    @just --list

# Launch the Jekyll website locally
serve:
    cd docs && bundle exec jekyll serve

# Build the Jekyll website
build:
    cd docs && bundle exec jekyll build

# Install Jekyll dependencies
install:
    cd docs && bundle install

# Launch the Java demo web app (Javalin)
demo:
    mvn -q test-compile exec:java -Dexec.mainClass=edu.montana.pika.web.TodoMVCDemo -Dexec.classpathScope=test
