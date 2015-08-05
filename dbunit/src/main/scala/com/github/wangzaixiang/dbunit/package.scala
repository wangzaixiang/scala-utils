package com.github.wangzaixiang

import scala.xml.Elem

/**
 * Created by wangzx on 15/7/21.
 */
package dbunit {

  sealed trait DataSetSyncMode

  // insert rows, throw exception on dupicate
  object INSERT extends DataSetSyncMode

  // update rows, throws exception when not exists
  object UPDATE extends DataSetSyncMode

  // perform an INSERT operation when not exists, or an UPDATE operation when exists
  object INSERT_UPDATE extends DataSetSyncMode

  // first clean all records then insert
  object REFRESH extends DataSetSyncMode



}