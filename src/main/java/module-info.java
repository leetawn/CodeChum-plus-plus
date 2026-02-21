module com.exception.ccpp {
    requires java.base;
    requires java.datatransfer;
    requires java.desktop;
    requires jdk.compiler;
    requires pty4j;
    requires org.fife.RSyntaxTextArea;
    requires kotlin.stdlib;
    requires com.formdev.flatlaf;
    requires com.kitfox.svgSalamander;
    requires java.compiler;
    requires com.fasterxml.jackson.annotation;
    requires tools.jackson.databind;
    exports com.exception.ccpp;
    exports com.exception.ccpp.FileManagement;
}