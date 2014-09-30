package org.neo4j.ogm.metadata;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.mapper.domain.bike.Bike;
import org.neo4j.ogm.mapper.domain.rulers.Ruler;
import org.neo4j.ogm.strategy.simple.SimpleClassDictionary;

import static junit.framework.Assert.assertEquals;

public class ClassDictionaryTest {

    private SimpleClassDictionary scd;

    @Before
    public void setUp() {
        scd = new SimpleClassDictionary();
        Class ruler = Ruler.class;
        Class bike = Bike.class;
    }

    @Test
    public void testFQNamespaceOfBikeDomain() {
        assertEquals("org.neo4j.ogm.mapper.domain.bike.Bike", scd.determineLeafClass("Bike"));
        assertEquals("org.neo4j.ogm.mapper.domain.bike.Wheel", scd.determineLeafClass("Wheel"));
        assertEquals("org.neo4j.ogm.mapper.domain.bike.Frame", scd.determineLeafClass("Frame"));
        assertEquals("org.neo4j.ogm.mapper.domain.bike.Saddle", scd.determineLeafClass("Saddle"));
    }

    @Test
    public void testInterfaceTaxa() {
        assertEquals(null, scd.determineLeafClass("Ruler"));
    }

    @Test
    public void testAbstractClassTaxa() {
        assertEquals(null, scd.determineLeafClass("Person"));
    }

    @Test
    public void testNoCommonLeafInTaxa() {
        assertEquals(null, scd.determineLeafClass("Daughter", "Son"));
    }

    @Test
    public void testMaleHeirIsLeafClassInAnyOrderingOfPrinceSonMaleHeirTaxa() {
        assertEquals("org.neo4j.ogm.mapper.domain.rulers.MaleHeir", scd.determineLeafClass("Son", "Prince", "MaleHeir"));
        assertEquals("org.neo4j.ogm.mapper.domain.rulers.MaleHeir", scd.determineLeafClass("Son", "MaleHeir", "Prince"));
        assertEquals("org.neo4j.ogm.mapper.domain.rulers.MaleHeir", scd.determineLeafClass("Prince", "Son", "MaleHeir"));
        assertEquals("org.neo4j.ogm.mapper.domain.rulers.MaleHeir", scd.determineLeafClass("Prince", "MaleHeir", "Son"));
        assertEquals("org.neo4j.ogm.mapper.domain.rulers.MaleHeir", scd.determineLeafClass("MaleHeir", "Son", "Prince"));
        assertEquals("org.neo4j.ogm.mapper.domain.rulers.MaleHeir", scd.determineLeafClass("MaleHeir", "Prince", "Son"));
    }

    @Test
    public void testLiskovSubstitutionPrinciple() {
        assertEquals("org.neo4j.ogm.mapper.domain.rulers.Daughter", scd.determineLeafClass("Daughter"));
        assertEquals("org.neo4j.ogm.mapper.domain.rulers.Princess", scd.determineLeafClass("Daughter", "Princess"));
    }

    @Test
    public void testAllNonMemberTaxa() {
        assertEquals(null, scd.determineLeafClass("Knight", "Baronet"));
    }

    @Test
    public void testNonMemberAndMemberTaxa() {
        assertEquals("org.neo4j.ogm.mapper.domain.rulers.Duke", scd.determineLeafClass("Knight", "Baronet", "Duke"));
    }

}
