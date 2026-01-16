mvn test-compile -q
mvn exec:java -Dexec.mainClass="de.in.lsp.ui.LogViewHeaderTest" -Dexec.classpathScope="test" -q 2>&1
