import org.junit.Test;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.FileSystemResource;

public class Mytest {
    @Test
    public void demo01() {
        XmlBeanFactory xmlBeanFactory = new XmlBeanFactory(new FileSystemResource("E:\\git_repository\\spring源码学习\\spring-framework3\\spring-framework-3.2.x\\using-springmvc\\src\\test\\resources\\applicationContext.xml"));
    }
}
