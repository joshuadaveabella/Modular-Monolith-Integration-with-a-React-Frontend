package edu.cit.abella.supplier;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import java.io.StringReader;
import java.io.StringWriter;

final class LsXml {

    private LsXml() {
    }

    static String marshal(Object value) {
        try {
            JAXBContext context = JAXBContext.newInstance(value.getClass());
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            StringWriter writer = new StringWriter();
            marshaller.marshal(value, writer);
            return writer.toString();
        } catch (JAXBException e) {
            throw new IllegalStateException("Failed to marshal " + value.getClass().getSimpleName(), e);
        }
    }

    static <T> T unmarshal(String xml, Class<T> type) {
        try {
            JAXBContext context = JAXBContext.newInstance(type);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return type.cast(unmarshaller.unmarshal(new StringReader(xml)));
        } catch (JAXBException e) {
            throw new IllegalStateException("Failed to unmarshal into " + type.getSimpleName()
                    + ". Raw body: " + xml, e);
        }
    }
}
