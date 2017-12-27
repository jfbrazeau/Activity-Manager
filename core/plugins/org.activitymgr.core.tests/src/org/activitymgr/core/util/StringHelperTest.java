package org.activitymgr.core.util;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

public class StringHelperTest extends TestCase {

	public void test0() {
		assertEquals("00", StringHelper.toHex((byte)0));
	}

	public void test10() {
		assertEquals("0A", StringHelper.toHex((byte)10));
	}

	public void test255() {
		assertEquals("FF", StringHelper.toHex((byte)255));
	}

	public void test0x00() {
		assertEquals((byte) 0, StringHelper.toByte("0"));
	}

	public void test0x10() {
		assertEquals((byte) 10, StringHelper.toByte("0A"));
	}

	public void test0xFF() {
		assertEquals((byte) 255, StringHelper.toByte("FF"));
	}

	public void test1ToEntry() {
		assertEquals("0.01", StringHelper.hundredthToEntry(1));
	}

	public void test10ToEntry() {
		assertEquals("0.10", StringHelper.hundredthToEntry(10));
	}

	public void test100ToEntry() {
		assertEquals("1.00", StringHelper.hundredthToEntry(100));
	}

	public void test123ToEntry() {
		assertEquals("1.23", StringHelper.hundredthToEntry(123));
	}

	public void test1234567890ToEntry() {
		assertEquals("12345678.90", StringHelper.hundredthToEntry(1234567890));
	}

	public void test0_00ToHundredth() throws StringFormatException {
		assertEquals(0, StringHelper.entryToHundredth("0.00"));
	}

	public void test0_01ToHundredth() throws StringFormatException {
		assertEquals(1, StringHelper.entryToHundredth("0.01"));
	}

	public void test0_10ToHundredth() throws StringFormatException {
		assertEquals(10, StringHelper.entryToHundredth("0.10"));
		assertEquals(10, StringHelper.entryToHundredth("0.1"));
	}

	public void test1_00ToHundredth() throws StringFormatException {
		assertEquals(100, StringHelper.entryToHundredth("1.00"));
		assertEquals(100, StringHelper.entryToHundredth("1.0"));
		assertEquals(100, StringHelper.entryToHundredth("1"));
	}

	public void test01_00ToHundredth() throws StringFormatException {
		assertEquals(100, StringHelper.entryToHundredth("01.00"));
		assertEquals(100, StringHelper.entryToHundredth("01.0"));
		assertEquals(100, StringHelper.entryToHundredth("01"));
	}

	public void test1_23ToHundredth() throws StringFormatException {
		assertEquals(123, StringHelper.entryToHundredth("1.23"));
	}

	public void test12345678_90ToHundredth() throws StringFormatException {
		assertEquals(1234567890, StringHelper.entryToHundredth("12345678.90"));
		assertEquals(1234567890, StringHelper.entryToHundredth("12345678.9"));
	}

	public void test0_001ToHundredth() {
		try { 
			StringHelper.entryToHundredth("0.001");
			fail("3 digits is supposed to be too much");
		}
		catch (StringFormatException expected) { }
	}

	public void test1_234ToHundredth() {
		try { 
			StringHelper.entryToHundredth("1.234");
			fail("3 digits is supposed to be too much");
		}
		catch (StringFormatException expected) { }
	}

	public void testBase32() {
		Set<String> base32s = new HashSet<String>();
		for (int value = 0; value < 1024; value++) {
			String base32 = StringHelper.toBase32(value);
			assertNotNull(base32);
			assertEquals(2, base32.length());
			assertFalse(base32s.contains(base32));
			base32s.add(base32);
			int decoded = StringHelper.fromBase32(base32);
			assertEquals(value, decoded);
		}
	}

	public void testBase32Overflow() {
		StringHelper.toBase32(1023); // No overflow
		try {
			StringHelper.toBase32(1024); // Overflow
			fail("Illegal argument exception should have been raised");
		}
		catch (IllegalArgumentException expected) {
		}
	}

	public void testConvertCamelcase() {
		assertEquals("My Taylor Is Rich",
				StringHelper.camelCaseToPhrase("myTaylorIsRich"));
		assertEquals("My Taylor Is Rich",
				StringHelper.camelCaseToPhrase("my Taylor Is Rich"));
		assertEquals("My Taylor Is Rich",
				StringHelper.camelCaseToPhrase(" My  Taylor   Is   Rich  "));
	}

}
