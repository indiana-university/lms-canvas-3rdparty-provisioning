package edu.iu.uits.lms.provisioning.service;

import Services.ams.Guest;
import Services.ams.GuestInfo;
import Services.ams.GuestResponse;
import Services.ams.Service;
import Services.ams.ServiceLocator;
import Services.ams.ServiceSoap;
import edu.iu.uits.lms.provisioning.config.AmsConfig;
import lombok.extern.log4j.Log4j;
import org.apache.axis.client.Stub;
import org.apache.axis.message.SOAPHeaderElement;
import org.springframework.beans.factory.annotation.Autowired;

import javax.xml.soap.SOAPElement;
import java.net.URL;
import java.rmi.RemoteException;

/**
 * Created by jonrcook on 12/5/14.
 */
@org.springframework.stereotype.Service
@Log4j
public class AmsServiceImpl {

    @Autowired
    private AmsConfig amsConfig;

    private ServiceSoap serviceSoap;

    public String sendPasswordResetPin(String emailAddress) {
        try {
            return getServiceSoap().sendPasswordResetPin(emailAddress);
        } catch (RemoteException e) {
            log.error("A problem occurred when calling ams guest account service", e);
            throw new IllegalStateException("A problem occurred when calling ams guest account service.", e);
        }
    }

    public GuestInfo lookupGuestBySequence(String sequence) {
        try {
            return getServiceSoap().lookupGuestBySequence(sequence);
        } catch (RemoteException e) {
            log.error("A problem occurred when calling ams guest account service", e);
            throw new IllegalStateException("A problem occurred when calling ams guest account service.", e);
        }
    }

    public GuestInfo lookupGuestByEmail(String emailAddress) {
        try {
            GuestInfo guestInfo = getServiceSoap().lookupGuestByEmail(emailAddress);
            guestInfo = fixNames(guestInfo);
            return guestInfo;
        } catch (RemoteException e) {
            log.error("A problem occurred when calling ams guest account service", e);
            throw new IllegalStateException("A problem occurred when calling ams guest account service.", e);
        }
    }

    public GuestResponse createGuest(Guest guest) {
        try {
            return getServiceSoap().createGuest(guest);
        } catch (RemoteException e) {
            log.error("A problem occurred when calling ams guest account service", e);
            throw new IllegalStateException("A problem occurred when calling ams guest account service.", e);
        }
    }

    public boolean checkEmailAddress(String emailAddress) {
        boolean hasAccount = false;
        try {
            ServiceSoap soap = getServiceSoap();
            String result = soap.checkEmailAddress(emailAddress);

            if (!"false".equals(result)) {
                // found an email address
                hasAccount = true;
            }
        } catch (RemoteException e) {
            log.error("A problem occurred when calling ams guest account service", e);
            throw new IllegalStateException("A problem occurred when calling ams guest account service.", e);
        }
        return hasAccount;
    }

    private ServiceSoap getServiceSoap() {

        if(serviceSoap != null) {
            return serviceSoap;
        }

        try {
            String user = amsConfig.getUser();
            String password = amsConfig.getPassword();
            String amsServiceUrl = amsConfig.getUrl();

            if(user == null || password == null || amsServiceUrl == null) {
                log.error("user, password, amsServiceUrl cannot be null");
                throw new IllegalStateException("A server error occurred. If the problem continues, contact the service center");
            }

            Service service = new ServiceLocator();
            SOAPHeaderElement header = new SOAPHeaderElement("ams.Services", "AuthSoapHd");
            SOAPElement username = header.addChildElement("UserName");
            username.addTextNode(user);
            SOAPElement passwordSoap = header.addChildElement("Password");
            passwordSoap.addTextNode(password);

            URL url = new URL(amsServiceUrl);
            ServiceSoap soap = service.getServiceSoap(url);
            ((Stub) soap).setHeader(header);
            return soap;
        } catch (Exception e) {
            log.error("A problem occurred when calling ams guest account service", e);
            throw new RuntimeException("A problem occurred when calling ams guest account service", e);
        }
    }

    /**
     * The AMS service lookupGuestByEmail operation returns guestInfo with firstName and lastName switched. This method
     * swaps the names and returns the corrected guestInfo
     * @param guestInfo
     * @return
     */
    private GuestInfo fixNames(GuestInfo guestInfo) {
        String lastName = new String(guestInfo.getStringFirstName());
        String firstName = new String(guestInfo.getStringLastName());

        guestInfo.setStringFirstName(firstName);
        guestInfo.setStringLastName(lastName);
        return guestInfo;
    }
}
