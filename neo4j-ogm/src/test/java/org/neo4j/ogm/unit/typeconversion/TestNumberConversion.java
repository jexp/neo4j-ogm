package org.neo4j.ogm.unit.typeconversion;

import org.junit.Test;
import org.neo4j.ogm.domain.convertible.numbers.Account;
import org.neo4j.ogm.metadata.MetaData;
import org.neo4j.ogm.metadata.info.ClassInfo;
import org.neo4j.ogm.typeconversion.AttributeConverter;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestNumberConversion {

    private static final MetaData metaData = new MetaData("org.neo4j.ogm.domain.convertible.numbers");
    private static final ClassInfo accountInfo = metaData.classInfo("Account");

    @Test
    public void assertAccountFieldsHaveDefaultConverters() {
        assertTrue(accountInfo.propertyField("balance").hasConverter());
        assertTrue(accountInfo.propertyField("facility").hasConverter());

    }

    @Test
    public void assertAccountMethodsHaveDefaultConverters() {
        assertTrue(accountInfo.propertyGetter("balance").hasConverter());
        assertTrue(accountInfo.propertySetter("balance").hasConverter());

        assertTrue(accountInfo.propertyGetter("facility").hasConverter());
        assertTrue(accountInfo.propertySetter("facility").hasConverter());
    }

    @Test
    public void assertAccountBalanceConverterWorks() {

        AttributeConverter converter = accountInfo.propertyGetter("balance").converter();

        Account account = new Account(new BigDecimal("12345.67"), new BigInteger("1000"));
        assertEquals("12345.67", converter.toGraphProperty(account.getBalance()));

        account.setBalance((BigDecimal) converter.toEntityAttribute("34567.89"));
        assertEquals(new BigDecimal("34567.89"), account.getBalance());
    }

    @Test
    public void assertAccountFacilityConverterWorks() {

        AttributeConverter converter = accountInfo.propertyGetter("facility").converter();

        Account account = new Account(new BigDecimal("12345.67"), new BigInteger("1000"));
        assertEquals("1000", converter.toGraphProperty(account.getFacility()));

        account.setFacility((BigInteger) converter.toEntityAttribute("2000"));
        assertEquals(new BigInteger("2000"), account.getFacility());
    }

    @Test
    public void assertConvertingNullGraphPropertyWorksCorrectly() {
        AttributeConverter converter = accountInfo.propertyGetter("facility").converter();
        assertEquals(null, converter.toEntityAttribute(null));
    }

    @Test
    public void assertConvertingNullAttributeWorksCorrectly() {
        AttributeConverter converter = accountInfo.propertyGetter("facility").converter();
        assertEquals(null, converter.toGraphProperty(null));
    }
}
