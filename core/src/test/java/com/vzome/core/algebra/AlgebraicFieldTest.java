package com.vzome.core.algebra;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

/**
 * @author David Hall
 */
public class AlgebraicFieldTest {
    private final static Set<AlgebraicField> fields = new HashSet<>();
    
    static {
        fields.add (new PentagonField());
        fields.add (new RootTwoField());
        fields.add (new RootThreeField());
        fields.add (new HeptagonField());
        fields.add (new SnubDodecField());
    }
    
    public void testNotEqual() {
        int pass = 0;
        AlgebraicField last = null;
        for(AlgebraicField field : fields) {
            assertFalse(field.equals(last));
            pass++;
            last = field;
        }
        assertTrue(pass > 0);
        assertEquals(fields.size(), pass);
	}
        
    @Test
	public void testOrder() {
        int pass = 0;
        for(AlgebraicField field : fields) {
            assertTrue(field.getOrder() >= 2);
            pass++;
        }
        assertEquals(fields.size(), pass);
	}    

    	@Test
    	public void testReciprocal()
    	{
    		for( AlgebraicField field : fields ) {
    			try {
    				field .zero() .reciprocal() .evaluate();
    				fail( "Zero divide should throw an exception" );
    			} catch ( RuntimeException re ) {
    				assertEquals( "Denominator is zero", re .getMessage() );
    			}
    		}
    	}
}
