package com.vandenbreemen.sfs_extendable.overview

import com.vandenbreemen.mobilesecurestorage.android.sfs.SFSCredentials
import com.vandenbreemen.mobilesecurestoragemvp.Model

/**
 *
 * @author kevin
 */
class OverviewModel(credentials: SFSCredentials) : Model(credentials) {

    fun getFilesCount(): Int {
        return sfs.listFiles().size
    }

    override fun onClose() {

    }

    override fun setup() {

    }
}