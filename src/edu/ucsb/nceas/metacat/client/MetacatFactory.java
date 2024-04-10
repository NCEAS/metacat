package edu.ucsb.nceas.metacat.client;

import java.io.Reader;

/**
 *  This class is a factory which allows the caller to create an instance of
 *  a Metacat object for accessing a metacat server.
 */
public class MetacatFactory
{
    private static final String metacatClientClass = 
         "edu.ucsb.nceas.metacat.client.MetacatClient";

    /**
     *  Create a new instance of a Metacat object of raccessing a server.
     *
     *  @param metacatUrl the url location of the metacat server
     *  @throws MetacatInaccessibleException when the metacat server can not
     *                    be reached
     */
    public static Metacat createMetacatConnection(String metacatUrl) 
           throws MetacatInaccessibleException
    {
        Metacat m = null;
        try {
            Class c = Class.forName(metacatClientClass);
            m = (Metacat)c.newInstance();
        } catch (InstantiationException e) {
            throw new MetacatInaccessibleException(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new MetacatInaccessibleException(e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new MetacatInaccessibleException(e.getMessage());
        }

        m.setMetacatUrl(metacatUrl);

        return m;
    }
}
