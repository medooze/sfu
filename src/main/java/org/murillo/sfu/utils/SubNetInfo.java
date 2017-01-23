/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.murillo.sfu.utils;

import java.io.Serializable;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

/**
 *
 * @author Sergio
 */
@XmlType
@XmlAccessorType(XmlAccessType.NONE)
public class SubNetInfo implements Serializable {

    //Create private subnets
    private final static SubNetInfo loopback = new SubNetInfo(new byte[]{127,0,0,1}     ,8);
    private final static SubNetInfo classA   = new SubNetInfo(new byte[]{10,0,0,1}      ,8);
    private final static SubNetInfo classB   = new SubNetInfo(new byte[]{(byte)172,16,0,1}    ,12);
    private final static SubNetInfo classC   = new SubNetInfo(new byte[]{(byte)192,(byte)168,0,0}   ,16);

    public static boolean isPrivate(String ip) throws UnknownHostException {
        //Get address bytes
        byte[] addr =  Inet4Address.getByName(ip).getAddress();

        //Check
        return loopback.contains(addr) || classA.contains(addr) || classB.contains(addr) || classC.contains(addr);
    }

    private String subnet;
    private int mask;
    private int base;

    public SubNetInfo() {

    }

    public SubNetInfo(String cidr) throws UnknownHostException {
        try {
            //Split
            String[] info = cidr.split("/");
            //Split
            subnet = info[0];
            mask = Integer.parseInt(info[1]);
        } catch (Exception e) {
            throw new UnknownHostException(cidr);
        }
        //Get address bytes
        base = getBase(Inet4Address.getByName(subnet).getAddress(),mask);
    }

    public SubNetInfo(String subnet,int mask) throws UnknownHostException {
        //Store values
        this.subnet = subnet;
        this.mask = mask;
        //Get address bytes
        base = getBase(Inet4Address.getByName(subnet).getAddress(),mask);
    }

    public SubNetInfo(byte[] addr,int mask) {
        //Store values
        this.subnet = (((short)addr[0]) & 0xFF)+"."+(((short)addr[1]) & 0xFF)+"."+(((short)addr[2]) & 0xFF)+"."+(((short)addr[3]) & 0xFF);
        this.mask = mask;
        //Get address bytes
        base = getBase(addr,mask);
    }

    public boolean contains(String ip) throws UnknownHostException {
        //Get addres
        byte[] addr = Inet4Address.getByName(ip).getAddress();
        //Get bytes
        return contains(addr);
    }

    public boolean contains(byte[] addr) {
        //Get bytes
        return base==getBase(addr, mask);
    }

    private static int getBase(byte[] b,int mask) {
        int res = 0;
        //Check mask
        if (mask==0)
            //Matches everything
            return 0;
        //Add bytes
        res |=(((short)b[0]) & 0xFF)<<24;
        res |=(((short)b[1]) & 0xFF)<<16;
        res |=(((short)b[2]) & 0xFF)<<8;
        res |=((short)b[3]) & 0xFF;
        //Calculate mask
        res = res >>> (32-mask);
        //return it
        return res;
    }

    @XmlValue
    public String getCIDR() {
        //Convert to string
        return subnet + "/" + mask ;
    }

    @Override
    public String toString() {
        return subnet + "/" + mask ;
    }
}
