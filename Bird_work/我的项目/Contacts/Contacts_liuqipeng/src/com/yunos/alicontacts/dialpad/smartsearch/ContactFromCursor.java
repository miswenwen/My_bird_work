package com.yunos.alicontacts.dialpad.smartsearch;

public class ContactFromCursor {

	public String name = null;
	public String number = null;
	public long nameVersion = -1;
	public long rawContactId = -1;
	public long contactId = -1;

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder(64);
		result.append("{name=").append(name)
		      .append("; number=").append(number)
		      .append("; nameVersion=").append(nameVersion)
		      .append("; rawContactId=").append(rawContactId)
		      .append("; contactId=").append(contactId).append('}');
		return result.toString();
	}
}
