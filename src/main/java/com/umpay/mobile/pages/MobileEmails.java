package com.umpay.mobile.pages;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Fresh addresses for registration, delivered to one real mailbox.
 *
 * The same trick the UMPay web suite uses, and for the same reasons. A registered address
 * cannot be reused, so a run that signs up needs one nobody has used before; but the code
 * the app emails has to arrive somewhere a test can read it.
 *
 * Plus addressing gives both. "lawma195.infinity+m28082026093000@gmail.com" is a brand new
 * address as far as UMPay is concerned, while the mail itself is delivered straight to
 * lawma195.infinity@gmail.com - the tag is routing information for the recipient, not part
 * of the mailbox.
 *
 * It also lets the reader pick out this run's message. The IMAP reader matches on the
 * recipient, so a shared address means an older verification code can be read as though it
 * were this run's; a per-run tag makes that impossible.
 */
public final class MobileEmails {

	private MobileEmails() {
	}

	/**
	 * A fresh address built from a real one.
	 *
	 * Any tag already on the address is dropped first, so repeated runs do not stack them
	 * up into something the mail server will not accept.
	 */
	public static String unique(String baseEmail) {

		int at = baseEmail.indexOf("@");

		if (at < 0) {
			System.out.println("Not a valid email address, using it as it is: " + baseEmail);
			return baseEmail;
		}

		String local = baseEmail.substring(0, at);
		String domain = baseEmail.substring(at);

		if (local.contains("+")) {
			local = local.substring(0, local.indexOf("+"));
		}

		/*
		 * The tag is the bare timestamp, with no letter in front of it.
		 *
		 * This matches the UMPay web suite exactly, and the shape matters more than it
		 * looks. Reading the mailbox shows 249 messages from umpay.me, every delivered
		 * verification code addressed to a +ddMMyyyyHHmmss alias - while a registration
		 * sent to +m28082026085428, the mobile suite's old format with an m after the plus,
		 * produced no email at all.
		 *
		 * One run is not proof that the letter is the cause, but there is no reason to
		 * differ from the format that is known to be delivered.
		 */
		String unique = local + "+"
				+ new SimpleDateFormat("ddMMyyyyHHmmss").format(new Date()) + domain;

		System.out.println("Generated unique email address: " + unique
				+ " (delivered to " + local + domain + ")");

		return unique;

	}

	/** The mailbox a plus-tagged address is delivered to. */
	public static String mailboxFor(String address) {

		int at = address.indexOf("@");

		if (at < 0) {
			return address;
		}

		String local = address.substring(0, at);

		if (local.contains("+")) {
			local = local.substring(0, local.indexOf("+"));
		}

		return local + address.substring(at);

	}
}
