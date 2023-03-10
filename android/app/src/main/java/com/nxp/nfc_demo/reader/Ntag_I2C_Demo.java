/*
 ****************************************************************************
 * Copyright(c) 2014 NXP Semiconductors                                     *
 * All rights are reserved.                                                 *
 *                                                                          *
 * Software that is described herein is for illustrative purposes only.     *
 * This software is supplied "AS IS" without any warranties of any kind,    *
 * and NXP Semiconductors disclaims any and all warranties, express or      *
 * implied, including all implied warranties of merchantability,            *
 * fitness for a particular purpose and non-infringement of intellectual    *
 * property rights.  NXP Semiconductors assumes no responsibility           *
 * or liability for the use of the software, conveys no license or          *
 * rights under any patent, copyright, mask work right, or any other        *
 * intellectual property rights in or to any products. NXP Semiconductors   *
 * reserves the right to make changes in the software without notification. *
 * NXP Semiconductors also makes no representation or warranty that such    *
 * application will be suitable for the specified use without further       *
 * testing or modification.                                                 *
 *                                                                          *
 * Permission to use, copy, modify, and distribute this software and its    *
 * documentation is hereby granted, under NXP Semiconductors' relevant      *
 * copyrights in the software, without fee, provided that it is used in     *
 * conjunction with NXP Semiconductor products(UCODE I2C, NTAG I2C).        *
 * This  copyright, permission, and disclaimer notice must appear in all    *
 * copies of this code.                                                     *
 ****************************************************************************
 */
package com.nxp.nfc_demo.reader;

import java.io.IOException;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

import org.jetbrains.annotations.NotNull;
import org.ndeftools.EmptyRecord;
import org.ndeftools.Message;
import org.ndeftools.MimeRecord;
import org.ndeftools.Record;
import org.ndeftools.externaltype.AndroidApplicationRecord;
import org.ndeftools.wellknown.SmartPosterRecord;
import org.ndeftools.wellknown.TextRecord;
import org.ndeftools.wellknown.UriRecord;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import android.os.DeadObjectException;

import com.gknot.activities.NtagRawRecord;
import com.gknot.activities.SharedCodes;
import com.gknot.pseudo.InstanceDelegateListener;
import com.gknot.pseudo.OnNDEFReadListener;
import com.gknot.pseudo.OnNDEFWriteListener;
import com.gknot.pseudo.OnWriteEEPROMListener;
import com.gknot.pseudo.OnWriteSRAMListener;
import com.gknot.reader.FlutterNtag_I2C_Demo;
import com.nxp.nfc_demo.crypto.CRC32Calculator;
import com.nxp.nfc_demo.exceptions.CommandNotSupportedException;
import com.nxp.nfc_demo.exceptions.NotPlusTagException;
import com.nxp.nfc_demo.listeners.WriteEEPROMListener;
import com.nxp.nfc_demo.listeners.WriteSRAMListener;
import com.nxp.nfc_demo.reader.I2C_Enabled_Commands.Access_Offset;
import com.nxp.nfc_demo.reader.I2C_Enabled_Commands.CR_Offset;
import com.nxp.nfc_demo.reader.I2C_Enabled_Commands.NC_Reg_Func;
import com.nxp.nfc_demo.reader.I2C_Enabled_Commands.NS_Reg_Func;
import com.nxp.nfc_demo.reader.I2C_Enabled_Commands.PT_I2C_Offset;
import com.nxp.nfc_demo.reader.I2C_Enabled_Commands.R_W_Methods;
import com.nxp.nfc_demo.reader.I2C_Enabled_Commands.SR_Offset;
import com.nxp.nfc_demo.reader.Ntag_Get_Version.Prod;
import com.nxp.nfc_demo.reader.Ntag_I2C_Commands.Register;
import com.nxp.ntagi2cdemo.R;

//import com.nxp.nfc_demo.fragments.LedFragment;

import com.gknot.activities.AuthActivity.AuthStatus;
import com.gknot.activities.FlashMemoryActivity;
import com.gknot.activities.RegisterConfigActivity;
import com.gknot.activities.VersionInfoActivity;
import com.gknot.activities.RegisterSessionActivity;
import com.gknot.activities.PseudoMainActivity;

import com.gknot.fragments.LedFragment;
import com.gknot.fragments.NdefFragment;
import com.gknot.fragments.SpeedTestFragment;
import com.gknot.pseudo.UIDelegateListener;
import com.gknot.pseudo.SetBoardVersionListener;
import com.gknot.pseudo.ShowAlertListener;
import com.gknot.pseudo.ShowToastListener;

import io.flutter.plugin.common.MethodChannel;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;


/**
 * Class for the different Demos.
 *
 * @author NXP67729
 */

public class Ntag_I2C_Demo implements WriteEEPROMListener, WriteSRAMListener {
   static public SetBoardVersionListener onSetBoardVersionHandler;
   static public ShowAlertListener onShowAuthWriteConfigAlertHandler;
   static public ShowAlertListener onShowAlertHandler;
   static public ShowAlertListener onShowOkDialogueHandler;
   static public ShowAlertListener onShowOkDialogueForActionHandler;
   static public ShowToastListener onToastMakeTextHandler;
   static public InstanceDelegateListener onInstanceDelegateHandler;
   static public OnWriteSRAMListener onWriteSRAMHandler;
   static public OnWriteEEPROMListener onWriteEEPROMHandler;
   static public OnNDEFWriteListener onNDEFWriteHandler;
   static public OnNDEFReadListener onNDEFReadHandler;
   static public UIDelegateListener onLEDHandler;

   static public void setOnWriteSRAMHandler(OnWriteSRAMListener listener) {
      onWriteSRAMHandler = listener;
   }

   static public void setOnWriteEEPROMHandler(OnWriteEEPROMListener listener) {
      onWriteEEPROMHandler = listener;
   }

   static public void setBoardVersionHandler(SetBoardVersionListener listener) {
      onSetBoardVersionHandler = listener;
   }

   static public void setShowAlertHandler(ShowAlertListener listener) {
      onShowAlertHandler = listener;
   }

   static public void setShowOkDialogueHandler(ShowAlertListener listener) {
      onShowOkDialogueHandler = listener;
   }

   static public void setShowOkDialogueForActionHandler(ShowAlertListener listener) {
      onShowOkDialogueForActionHandler = listener;
   }

   static public void setToastMakeTextHandler(ShowToastListener listener) {
      onToastMakeTextHandler = listener;
   }

   static public void setShowAuthWriteConfigAlertHandler(ShowAlertListener listener) {
      onShowAuthWriteConfigAlertHandler = listener;
   }

   static public void setInstanceDelegateHandler(InstanceDelegateListener listener) {
      onInstanceDelegateHandler = listener;
   }

   static public void setOnNDEFWriteHandler(OnNDEFWriteListener listener) {
      onNDEFWriteHandler = listener;
   }

   static public void setOnNDEFReadHandler(OnNDEFReadListener listener) {
      onNDEFReadHandler = listener;
   }

   public I2C_Enabled_Commands reader;
   private Activity main;
   private Tag tag;

   /**
    * Taskreferences.
    */
   private LedTask lTask;
   private SRAMSpeedtestTask sramspeedtask;
   private EEPROMSpeedtestTask eepromspeedtask;
   private WriteEmptyNdefTask emptyNdeftask;
   private WriteDefaultNdefTask defaultNdeftask;
   private NDEFReadTask ndefreadtask;

   /**
    * DEFINES.
    */
   private static final int LAST_FOUR_BYTES = 4;
   private static final int DELAY_TIME = 100;
   private static final int TRAILS = 300;
   private static final int DATA_SEND_BYTE = 12;
   private static final int VERSION_BYTE = 63;
   private static final int GET_VERSION_NR = 12;
   private static final int GET_FW_NR = 28;
   private static final int THREE_BYTES = 3;
   private static final int PAGE_SIZE = 4096;

   /**
    * Constructor.
    *
    * @param tag  Tag with which the Demos should be performed
    * @param main MainActivity
    */
   public Ntag_I2C_Demo(Tag tag, final Activity main, final byte[] passwd, final int authStatus) {
      try {
         if (tag == null) {
            this.main = null;
            this.tag = null;
            return;
         }
         this.main = main;
         this.tag = tag;

         reader = I2C_Enabled_Commands.get(tag);

         if (reader == null) {
            String message = "The Tag could not be identified or this NFC device does not "
                    + "support the NFC Forum commands needed to access this tag";
            String title = "Communication failed";
            showAlert(message, title);
         } else {
            reader.connect();
         }

         Ntag_Get_Version.Prod prod = reader.getProduct();

         if (!prod.equals(Ntag_Get_Version.Prod.Unknown)) {
            if (prod.equals(Ntag_Get_Version.Prod.NTAG_I2C_1k_Plus)
                    || prod.equals(Ntag_Get_Version.Prod.NTAG_I2C_2k_Plus)) {
               // Auth status gets lost after resetting the demo when we
               // obtain the product we are dealing with
               if (authStatus == AuthStatus.Authenticated.getValue()) {
                  reader.authenticatePlus(passwd);
               }
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
         SharedCodes.Companion.log("NDemo", e.toString());

      }
      if (onInstanceDelegateHandler != null)
         onInstanceDelegateHandler.onDelegateInstance(this);
   }

   private void showAlert(final String message, final String title) {
      if (onShowAlertHandler != null) {
         onShowAlertHandler.onShowAlert(message, title, this);
         return;
      }

      //fixme
      main.runOnUiThread(new Runnable() {
         @Override
         public void run() {
            new AlertDialog.Builder(main)
                    .setMessage(message)
                    .setTitle(title)
                    .setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                               @Override
                               public void onClick(DialogInterface dialog,
                                                   int which) {

                               }
                            }).show();
         }
      });

   }

   /**
    * Checks if the tag is still connected based on the previously detected reader.
    *
    * @return Boolean indicating tag connection
    */
   public boolean isConnected() {
      try {
         return reader.isConnected();
      } catch (Exception   e) {
         e.printStackTrace();
         Objects.requireNonNull(FlutterNtag_I2C_Demo.getInstance()).onNFCServiceDead();
         return false;
      }
   }

   /**
    * Checks if the tag is still connected based on the tag.
    *
    * @return Boolean indicating tag presence
    */
   public static boolean isTagPresent(Tag tag) {
      final Ndef ndef = Ndef.get(tag);
      if (ndef != null && !ndef.getType().equals("android.ndef.unknown")) {
         try {
            ndef.connect();
            final boolean isConnected = ndef.isConnected();
            ndef.close();
            return isConnected;
         } catch (final IOException e) {
            e.printStackTrace();
            SharedCodes.Companion.log("NDemo", e.toString());

            return false;
         }
      } else {
         final NfcA nfca = NfcA.get(tag);
         if (nfca != null) {
            try {
               nfca.connect();
               final boolean isConnected = nfca.isConnected();
               nfca.close();

               return isConnected;
            } catch (final IOException e) {
               e.printStackTrace();
               SharedCodes.Companion.log("NDemo", e.toString());

               return false;
            }
         } else {
            final NfcB nfcb = NfcB.get(tag);
            if (nfcb != null) {
               try {
                  nfcb.connect();
                  final boolean isConnected = nfcb.isConnected();
                  nfcb.close();
                  return isConnected;
               } catch (final IOException e) {
                  e.printStackTrace();
                  SharedCodes.Companion.log("NDemo", e.toString());

                  return false;
               }
            } else {
               final NfcF nfcf = NfcF.get(tag);
               if (nfcf != null) {
                  try {
                     nfcf.connect();
                     final boolean isConnected = nfcf.isConnected();
                     nfcf.close();
                     return isConnected;
                  } catch (final IOException e) {
                     e.printStackTrace();
                     SharedCodes.Companion.log("NDemo", e.toString());

                     return false;
                  }
               } else {
                  final NfcV nfcv = NfcV.get(tag);
                  if (nfcv != null) {
                     try {
                        nfcv.connect();
                        final boolean isConnected = nfcv.isConnected();
                        nfcv.close();
                        return isConnected;
                     } catch (final IOException e) {
                        e.printStackTrace();
                        SharedCodes.Companion.log("NDemo", e.toString());

                        return false;
                     }
                  } else {
                     return false;
                  }
               }
            }
         }
      }
   }

   /**
    * Finish all tasks.
    */
   public void finishAllTasks() {
      LEDFinish();
      SRAMSpeedFinish();
      EEPROMSpeedFinish();
      WriteEmptyNdefFinish();
      NDEFReadFinish();
   }

   /**
    * Checks if the demo is ready to be executed.
    *
    * @return Boolean indicating demo readiness
    */
   public boolean isReady() {
      if (tag != null && reader != null) {
         return true;
      }
      return false;
   }

   /**
    * Get the product from reader.
    *
    * @return product
    */
   public Prod getProduct() throws IOException {
      return reader.getProduct();
   }

   private void onSetBoardVersion(String ver, String fwver) {
      if (onSetBoardVersionHandler == null) {
         VersionInfoActivity.setBoardVersion(ver);
         VersionInfoActivity.setBoardFWVersion(fwver);
         return;
      }
      onSetBoardVersionHandler.onSetBoardVersion(ver, fwver, this);
   }

   /**
    * Set the current board version.
    */
   public void setBoardVersion() throws IOException, FormatException,
           CommandNotSupportedException {

      byte[] dataTx = new byte[reader.getSRAMSize()];
      byte[] dataRx = new byte[reader.getSRAMSize()];

      dataTx[reader.getSRAMSize() - LAST_FOUR_BYTES] = 'V';

      if (!((reader.getSessionRegister(SR_Offset.NC_REG) & NC_Reg_Func.PTHRU_ON_OFF.getValue())
              == NC_Reg_Func.PTHRU_ON_OFF.getValue())) {
         onSetBoardVersion("No Board attached", "No Board attached");
         return;
      }

      try {
         reader.waitforI2Cread(DELAY_TIME);
      } catch (TimeoutException e1) {
         e1.printStackTrace();
         onSetBoardVersion("No Board attached", "No Board attached");
         return;
      }

      reader.writeSRAMBlock(dataTx, null);

      for (int i = 0; i < TRAILS; i++) {
         if (((reader.getSessionRegister(SR_Offset.NS_REG) & NS_Reg_Func.SRAM_RF_READY
                 .getValue()) == NS_Reg_Func.SRAM_RF_READY.getValue())) {
            break;
         }
         try {
            Thread.sleep(1);
         } catch (InterruptedException e) {
            e.printStackTrace();
            SharedCodes.Companion.log("NDemo", e.toString());

         }
      }

      if (!((reader.getSessionRegister(SR_Offset.NS_REG) & NS_Reg_Func.SRAM_RF_READY
              .getValue()) == NS_Reg_Func.SRAM_RF_READY.getValue())) {
         onSetBoardVersion("1.0", "1.0");
         return;
      }

      dataRx = reader.readSRAMBlock();

      String boardVersion;
      String boardFWVersion;
      // Check if Data was send, else it is a ExplorerBoard FW
      if (dataRx[DATA_SEND_BYTE] == 0) {
         String version = Integer.toHexString((dataRx[VERSION_BYTE] >> LAST_FOUR_BYTES)
                 & (byte) 0x0F)
                 + "." + Integer.toHexString(dataRx[VERSION_BYTE] & (byte) 0x0F);
         boardVersion = version;
         boardFWVersion = version;
      } else {
         boardVersion = new String(dataRx, GET_VERSION_NR, THREE_BYTES);
         boardFWVersion = new String(dataRx, GET_FW_NR, THREE_BYTES);
      }
      onSetBoardVersion(boardVersion, boardFWVersion);
   }

   /**
    * Resets the tag to its delivery values (including config registers).
    *
    * @return Boolean indicating success or error
    */
   public int resetTagMemory() {
      int bytesWritten = 0;

      try {
         bytesWritten = reader.writeDeliveryNdef();
      } catch (Exception e) {
         e.printStackTrace();
         SharedCodes.Companion.log("NDemo", e.toString());

         bytesWritten = -1;
      }
      if (bytesWritten == 0) {
         showDemoNotSupportedAlert();
      } else {
         byte NC_REG = (byte) 0x01;
         byte LD_Reg = (byte) 0x00;
         byte SM_Reg = (byte) 0xF8;
         byte WD_LS_Reg = (byte) 0x48;
         byte WD_MS_Reg = (byte) 0x08;
         byte I2C_CLOCK_STR = (byte) 0x01;
         // If we could reset the memory map, we should be able to write the config registers
         try {
            reader.writeConfigRegisters(NC_REG, LD_Reg,
                    SM_Reg, WD_LS_Reg, WD_MS_Reg, I2C_CLOCK_STR);
         } catch (Exception e) {
            //Toast.makeText(main, "Error writing configuration registers", Toast.LENGTH_LONG).show();
            toastText(main, "Error writing configuration registers", Toast.LENGTH_LONG);
            e.printStackTrace();
            SharedCodes.Companion.log("NDemo", e.toString());

            bytesWritten = -1;
         }

         try {
            Ntag_Get_Version.Prod prod = reader.getProduct();

            if (prod.equals(Ntag_Get_Version.Prod.NTAG_I2C_1k_Plus)
                    || prod.equals(Ntag_Get_Version.Prod.NTAG_I2C_2k_Plus)) {
               byte AUTH0 = (byte) 0xFF;
               byte ACCESS = (byte) 0x00;
               byte PT_I2C = (byte) 0x00;
               reader.writeAuthRegisters(AUTH0, ACCESS, PT_I2C);
            }
         } catch (Exception e) {
            //Toast.makeText(main, "Error writing authentication registers", Toast.LENGTH_LONG).show();
            toastText(main, "Error writing authentication registers", Toast.LENGTH_LONG);
            e.printStackTrace();
            SharedCodes.Companion.log("NDemo", e.toString());

            bytesWritten = -1;
         }
      }
      return bytesWritten;
   }

   /**
    * Not implemented in the Firmware side.
    * Code just in case for future releases.
    */
//	public boolean resetTag() {
//		byte[] DataTx = new byte[reader.getSRAMSize()];
//		byte[] DataRx = new byte[reader.getSRAMSize()];
//		long RegTimeOutStart = System.currentTimeMillis();
//
//		try {
//			do {
//
//				if (reader.checkPTwritePossible()) {
//					break;
//				}
//
//				long RegTimeOut = System.currentTimeMillis();
//				RegTimeOut = RegTimeOut - RegTimeOutStart;
//				if (!(RegTimeOut < 5000))
//					return false;
//
//			} while (true);
//
//			DataTx[reader.getSRAMSize() - 4] = 'R';
//
//			// wait to prevent that a RF communication is
//			// at the same time as ??C I2C
//			Thread.sleep(10);
//
//			reader.waitforI2Cread(100);
//
//			reader.writeSRAMBlock(DataTx);
//
//			// wait to give the ??C time to reset the Tag
//			Thread.sleep(50);
//
//			reader.waitforI2Cwrite(100);
//
//			DataRx = reader.readSRAMBlock();
//
//			if (DataRx[reader.getSRAMSize() - 4] == 'A'
//					&& DataRx[reader.getSRAMSize() - 3] == 'C'
//					&& DataRx[reader.getSRAMSize() - 2] == 'K')
//				return true;
//			else
//				return false;
//		} catch (Exception e) {
//			e.printStackTrace();

//			return false;
//		}
//	}


   /**
    * Builds a String array for the Registers.
    *
    * @param register []byte of the Registers
    * @return String Array
    * @throws IOException     description
    * @throws FormatException description
    */
   private Ntag_I2C_Registers getRegister_Settings(byte[] register) throws IOException, FormatException {
      boolean custom = SharedCodes.Companion.getCustomWrite();
      Ntag_I2C_Registers answer = _getRegister_Settings(register);
      // read NDEF Message
      NdefMessage message;
      NtagRawRecord record = null;
      try {
         if (custom) {
            SharedCodes.Companion.log("readNDEFCustom", "");
            record = reader.readNDEFCustom();
            if (record.getMessage() == null){
               answer.NDEF_Message = "";
               answer.NDEF_Message_BYTES = new byte[0];
               answer.NDEF_RAW_RECORD = record;
            }else{
               message = record.getMessage();
               SharedCodes.Companion.log("readNDEFCustom", "valid ndefmessage");
               String NDEFText = new String(message.getRecords()[0].getPayload(), StandardCharsets.US_ASCII);
               NDEFText = NDEFText.subSequence(3, NDEFText.length()).toString();

               int shiftBits = (3 + SharedCodes.Companion.getShiftBits());
               byte[] payload = message.getRecords()[0].getPayload().clone();
               byte[] NDEFBytes = new byte[payload.length - shiftBits];
               System.arraycopy(payload, shiftBits, NDEFBytes, 0, NDEFBytes.length);

               SharedCodes.Companion.log("NDEFText raw", NDEFText);

               answer.NDEF_Message = NDEFText;
               answer.NDEF_Message_BYTES = NDEFBytes;
               answer.NDEF_RAW_RECORD = record;
            }

         } else {
            SharedCodes.Companion.log("readNDEF", "");
            message = reader.readNDEF();

            String NDEFText = new String(message.getRecords()[0].getPayload(), StandardCharsets.US_ASCII);
            NDEFText = NDEFText.subSequence(3, NDEFText.length()).toString();

            int shiftBits = (3 + SharedCodes.Companion.getShiftBits());
            byte[] payload = message.getRecords()[0].getPayload().clone();
            byte[] NDEFBytes = new byte[payload.length - shiftBits];
            System.arraycopy(payload, shiftBits, NDEFBytes, 0, NDEFBytes.length);

            SharedCodes.Companion.log("NDEFText raw", NDEFText);

            answer.NDEF_Message = NDEFText;
            answer.NDEF_Message_BYTES = NDEFBytes;
         }
      } catch (Exception e) {
         e.printStackTrace();
         SharedCodes.Companion.log("NDemo", e.toString());
         if (custom){
            answer.NDEF_Message = main.getString(R.string.No_NDEF);
            answer.NDEF_Message_BYTES = new byte[0];
            answer.NDEF_RAW_RECORD    = record;
         }else{
            answer.NDEF_Message = main.getString(R.string.No_NDEF);
            answer.NDEF_Message_BYTES = new byte[0];
         }
      }
      return answer;
   }

   private Ntag_I2C_Registers _getRegister_Settings(byte[] register) throws IOException, FormatException{
      Boolean custom = SharedCodes.Companion.getCustomWrite();
      Ntag_I2C_Registers answer = new Ntag_I2C_Registers();
      Ntag_Get_Version.Prod prod = reader.getProduct();

      //R.string.xxxx -> value generated from strings.xml
      if (!prod.equals(Ntag_Get_Version.Prod.Unknown)) {
         if (prod.equals(Ntag_Get_Version.Prod.NTAG_I2C_1k)) {
            answer.Manufacture = main.getString(R.string.ic_prod_ntagi2c_1k);
         } else if (prod.equals(Ntag_Get_Version.Prod.NTAG_I2C_2k)) {
            answer.Manufacture = main.getString(R.string.ic_prod_ntagi2c_2k);
         } else if (prod.equals(Ntag_Get_Version.Prod.NTAG_I2C_1k_Plus)) {
            answer.Manufacture = main.getString(R.string.ic_prod_ntagi2c_1k_Plus);
         } else if (prod.equals(Ntag_Get_Version.Prod.NTAG_I2C_2k_Plus)) {
            answer.Manufacture = main.getString(R.string.ic_prod_ntagi2c_2k_Plus);
         }
         answer.Mem_size = prod.getMemsize();
      } else {
         answer.Manufacture = "";
         answer.Mem_size = 0;
      }

      byte NC_Reg = register[SR_Offset.NC_REG.getValue()];

      // check I2C_RST_ON_OFF
      if ((NC_Reg & NC_Reg_Func.I2C_RST_ON_OFF.getValue()) == NC_Reg_Func.I2C_RST_ON_OFF
              .getValue()) {
         answer.I2C_RST_ON_OFF = true;
      } else {
         answer.I2C_RST_ON_OFF = false;
      }

      // check FD_OFF
      byte tmpReg = (byte) (NC_Reg & NC_Reg_Func.FD_OFF.getValue());
      if (tmpReg == (0x30)) {
         answer.FD_OFF = main.getString(R.string.FD_OFF_ON_11);
      }
      if (tmpReg == (0x20)) {
         answer.FD_OFF = main.getString(R.string.FD_OFF_ON_10);
      }
      if (tmpReg == (0x10)) {
         answer.FD_OFF = main.getString(R.string.FD_OFF_ON_01);
      }
      if (tmpReg == (0x00)) {
         answer.FD_OFF = main.getString(R.string.FD_OFF_ON_00);
      }

      // check FD_ON
      tmpReg = (byte) (NC_Reg & NC_Reg_Func.FD_ON.getValue());
      if (tmpReg == (0x0c)) {
         answer.FD_ON = main.getString(R.string.FD_OFF_ON_11);
      }
      if (tmpReg == (0x08)) {
         answer.FD_ON = main.getString(R.string.FD_OFF_ON_10);
      }
      if (tmpReg == (0x04)) {
         answer.FD_ON = main.getString(R.string.FD_OFF_ON_01);
      }
      if (tmpReg == (0x00)) {
         answer.FD_ON = main.getString(R.string.FD_OFF_ON_00);
      }

      // Last NDEF Page
      answer.LAST_NDEF_PAGE = (0x00000FF & register[SR_Offset.LAST_NDEF_PAGE
              .getValue()]);

      byte NS_Reg = register[SR_Offset.NS_REG.getValue()];

      // check NDEF_DATA_READ
      if ((NS_Reg & NS_Reg_Func.NDEF_DATA_READ.getValue()) == NS_Reg_Func.NDEF_DATA_READ
              .getValue()) {
         answer.NDEF_DATA_READ = true;
      } else {
         answer.NDEF_DATA_READ = false;
      }

      // check RF_FIELD
      if ((NS_Reg & NS_Reg_Func.RF_FIELD_PRESENT.getValue()) == NS_Reg_Func.RF_FIELD_PRESENT
              .getValue()) {
         answer.RF_FIELD_PRESENT = true;
      } else {
         answer.RF_FIELD_PRESENT = false;
      }

      // check PTHRU_ON_OFF
      if ((NC_Reg & (byte) NC_Reg_Func.PTHRU_ON_OFF.getValue()) == NC_Reg_Func.PTHRU_ON_OFF
              .getValue()) {
         answer.PTHRU_ON_OFF = true;
      } else {
         answer.PTHRU_ON_OFF = false;
      }

      // check I2C_LOCKED
      if ((NS_Reg & NS_Reg_Func.I2C_LOCKED.getValue()) == NS_Reg_Func.I2C_LOCKED
              .getValue()) {
         answer.I2C_LOCKED = true;
      } else {
         answer.I2C_LOCKED = false;
      }

      // check RF_LOCK
      if ((NS_Reg & NS_Reg_Func.RF_LOCKED.getValue()) == NS_Reg_Func.RF_LOCKED
              .getValue()) {
         answer.RF_LOCKED = true;
      } else {
         answer.RF_LOCKED = false;
      }

      // check check SRAM_I2C_Ready
      if ((NS_Reg & NS_Reg_Func.SRAM_I2C_READY.getValue()) == NS_Reg_Func.SRAM_I2C_READY
              .getValue()) {
         answer.SRAM_I2C_READY = true;
      } else {
         answer.SRAM_I2C_READY = false;
      }

      // check SRAM_RF_READY
      tmpReg = (byte) (NS_Reg & NS_Reg_Func.SRAM_RF_READY.getValue());
      if ((NS_Reg & NS_Reg_Func.SRAM_RF_READY.getValue()) == NS_Reg_Func.SRAM_RF_READY
              .getValue()) {
         answer.SRAM_RF_READY = true;
      } else {
         answer.SRAM_RF_READY = false;
      }

      // check PTHRU_DIR
      tmpReg = (byte) (NC_Reg & (byte) 0x01);
      if (tmpReg == (0x01)) {
         answer.PTHRU_DIR = true;
      } else {
         answer.PTHRU_DIR = false;
      }

      // SM_Reg
      answer.SM_Reg = (0x00000FF & register[SR_Offset.SM_REG.getValue()]);

      // WD_LS_Reg
      answer.WD_LS_Reg = (0x00000FF & register[SR_Offset.WDT_LS.getValue()]);

      // WD_MS_Reg
      answer.WD_MS_Reg = (0x00000FF & register[SR_Offset.WDT_MS.getValue()]);

      // check SRAM_MIRROR_ON_OFF
      if ((NC_Reg & NC_Reg_Func.SRAM_MIRROR_ON_OFF.getValue()) == NC_Reg_Func.SRAM_MIRROR_ON_OFF
              .getValue()) {
         answer.SRAM_MIRROR_ON_OFF = true;
      } else {
         answer.SRAM_MIRROR_ON_OFF = false;
      }

      // I2C_CLOCK_STR
      if (register[SR_Offset.I2C_CLOCK_STR.getValue()] == 1) {
         answer.I2C_CLOCK_STR = true;
      } else {
         answer.I2C_CLOCK_STR = false;
      }
      return answer;
   }

   private Ntag_I2C_Registers getRegister_SettingsCustomBytes(byte[] register, byte startAt, int length) throws IOException, FormatException {
      Ntag_I2C_Registers answer = _getRegister_Settings(register);
      NdefMessage message;
      NtagRawRecord record = null;
      byte[] NDEFBytes;
      try {

//         if (reader instanceof MinimalNtag_I2C_Commands){
//            NDEFBytes = reader.readEEPROMCustomBytes(startAt, startAt + length / 4);
//         }else{
//            NDEFBytes = reader.readEEPROMCustomBytes(startAt, startAt + length);
//         }

         NDEFBytes = reader.readEEPROMCustomBytes(startAt, startAt + length / reader.blockSize);
         answer.NDEF_Message = "";
         answer.NDEF_RAW_RECORD = record = new NtagRawRecord(
                 length, false, 0, length, NDEFBytes, null
         );
         answer.NDEF_Message_BYTES = new byte[0];
         SharedCodes.Companion.log(
           "getRegister_SettingsCustomBytes", "NDEFBytes: " + SharedCodes.Companion.join(NDEFBytes));

      } catch (Exception e) {
         e.printStackTrace();
         SharedCodes.Companion.log("NDemo", e.toString());
         answer.NDEF_Message = main.getString(R.string.No_NDEF);
         answer.NDEF_Message_BYTES = new byte[0];
         answer.NDEF_RAW_RECORD    = record;
      }
      return answer;
   }

   private Ntag_I2C_Registers getRegister_SettingsCustom(byte[] register, byte startAt) throws IOException, FormatException {
      Ntag_I2C_Registers answer = _getRegister_Settings(register);
      NdefMessage message;
      NtagRawRecord record = null;
      try {
         SharedCodes.Companion.log("readNDEFCustom", "");
         record = reader.readNDEFCustom();
         if (record.getMessage() == null){
            answer.NDEF_Message = "";
            answer.NDEF_Message_BYTES = new byte[0];
            answer.NDEF_RAW_RECORD = record;
         }else{
            message = record.getMessage();
            SharedCodes.Companion.log("readNDEFCustom", "valid ndefmessage");
            String NDEFText = new String(message.getRecords()[0].getPayload(), StandardCharsets.US_ASCII);
            NDEFText = NDEFText.subSequence(3, NDEFText.length()).toString();

            int shiftBits = (3 + SharedCodes.Companion.getShiftBits());
            byte[] payload = message.getRecords()[0].getPayload().clone();
            byte[] NDEFBytes = new byte[payload.length - shiftBits];
            System.arraycopy(payload, shiftBits, NDEFBytes, 0, NDEFBytes.length);

            SharedCodes.Companion.log("NDEFText raw", NDEFText);

            answer.NDEF_Message = NDEFText;
            answer.NDEF_Message_BYTES = NDEFBytes;
            answer.NDEF_RAW_RECORD = record;
         }
      } catch (Exception e) {
         e.printStackTrace();
         SharedCodes.Companion.log("NDemo", e.toString());
         answer.NDEF_Message = main.getString(R.string.No_NDEF);
         answer.NDEF_Message_BYTES = new byte[0];
         answer.NDEF_RAW_RECORD    = record;
      }
      return answer;
   }

   /**
    * Builds a String array for the NTAG I2C Plus Auth Register.
    *
    * @param auth0register  Byte Array of the Registers
    * @param pti2cRegister  description
    * @param accessRegister description
    * @return String Array
    * @throws IOException     description
    * @throws FormatException description
    */
   private Ntag_I2C_Plus_Registers getPlusAuth_Settings(byte[] auth0register,
                                                        byte[] accessRegister,
                                                        byte[] pti2cRegister)
           throws IOException, FormatException {
      Ntag_I2C_Plus_Registers answerPlus = new Ntag_I2C_Plus_Registers();

      //Auth0 Register
      answerPlus.auth0 = (0x00000FF & auth0register[3]);

      //Access Register
      if ((0x0000080 & accessRegister[0]) >> Access_Offset.NFC_PROT.getValue() == 1) {
         answerPlus.nfcProt = true;
      } else {
         answerPlus.nfcProt = false;
      }
      if ((0x0000020 & accessRegister[0]) >> Access_Offset.NFC_DIS_SEC1.getValue() == 1) {
         answerPlus.nfcDisSec1 = true;
      } else {
         answerPlus.nfcDisSec1 = false;
      }
      answerPlus.authlim = (0x0000007 & accessRegister[0]);

      //PT I2C Register
      if ((0x0000008 & pti2cRegister[0]) >> PT_I2C_Offset.K2_PROT.getValue() == 1) {
         answerPlus.k2Prot = true;
      } else {
         answerPlus.k2Prot = false;
      }
      if ((0x0000004 & pti2cRegister[0]) >> PT_I2C_Offset.SRAM_PROT.getValue() == 1) {
         answerPlus.sram_prot = true;
      } else {
         answerPlus.sram_prot = false;
      }
      answerPlus.i2CProt = (0x0000003 & pti2cRegister[0]);
      return answerPlus;
   }

   private void toastText(Activity main, String text, int time) {
      if (onToastMakeTextHandler == null) {
         Toast.makeText(main, text, time).show();
         return;
      }
      onToastMakeTextHandler.onToastMakeText(text, time, this);
   }

   /**
   * Read out the session registers.
   */
   public void readSessionRegisters() throws CommandNotSupportedException {
      boolean custom = SharedCodes.Companion.getCustomWrite();

//      FlutterNtag_I2C_Demo.getInstance().onReading();
//      NdefFragment.setAnswer("read tag failed\n", false, NdefFragment.Dart.READ_LOST);
//      return;

      if (custom) {
         SharedCodes.Companion.log("DEMO", "ReadSessionRegistersCustom");
      } else {
         SharedCodes.Companion.log("DEMO", "ReadSessionRegistersNormal");
      }
      try {
         if (custom){
            FlutterNtag_I2C_Demo.getInstance().onReading();
            byte start = 0x10;
            byte[] sessionRegisters = reader.getSessionRegisters();
            Ntag_I2C_Registers answer = getRegister_SettingsCustomBytes(sessionRegisters, start, NtagRawRecord.Companion.getDataLength());
            RegisterSessionActivity.SetAnswer(answer, main);
         }else{
            byte[] sessionRegisters = reader.getSessionRegisters();
            Ntag_I2C_Registers answer = getRegister_Settings(sessionRegisters);
            RegisterSessionActivity.SetAnswer(answer, main);
         }
         //toastText(main, "read tag successfully done", Toast.LENGTH_LONG) ;
      } catch (CommandNotSupportedException e) {
         e.printStackTrace();
         SharedCodes.Companion.log("NDemo", e.toString());
         throw e;
      } catch (IOException e) {
         e.printStackTrace();
         SharedCodes.Companion.log("NDemo", e.toString());
         NdefFragment.setAnswer("read tag failed\n" + e.toString(), false, NdefFragment.Dart.READ_LOST);
      } catch (Exception e) {
         e.printStackTrace();
         SharedCodes.Companion.log("NDemo", e.toString());
         toastText(main, "read tag failed\n" + e.toString(), Toast.LENGTH_LONG);
         NdefFragment.setAnswer("read tag failed\n" + e.toString(), false, NdefFragment.Dart.READ_LOST);
      }
   }

   public void readSessionRegistersCustom(byte startAt, int length,  MethodChannel.Result result) throws CommandNotSupportedException {
      SharedCodes.Companion.log("DEMO", "readSessionRegistersCustom");
      try {
         //FlutterNtag_I2C_Demo.getInstance().onReading();
         byte[] sessionRegisters = reader.getSessionRegisters();
         Ntag_I2C_Registers answer = getRegister_SettingsCustomBytes(sessionRegisters, startAt, length);
         Map ret = RegisterSessionActivity.Companion.initAnswer(answer, main);
         if (result != null)
            result.success(ret);
      } catch (CommandNotSupportedException e) {
         e.printStackTrace();
         SharedCodes.Companion.log("NDemo", e.toString());
         NdefFragment.setAnswer("read tag failed\n" + e.toString(), false, NdefFragment.Dart.READ_LOST);
         Map<String, String> map = new HashMap<>();
         map.put("error", e.toString());
         if (result != null)
            result.success(map);
      } catch (IOException e) {
         e.printStackTrace();
         SharedCodes.Companion.log("NDemo", e.toString());
         NdefFragment.setAnswer("read tag failed\n" + e.toString(), false, NdefFragment.Dart.READ_LOST);
         Map<String, String> map = new HashMap<>();
         map.put("error", e.toString());
         if (result != null)
            result.success(map);
      } catch (Exception e) {
         e.printStackTrace();
         SharedCodes.Companion.log("NDemo", e.toString());
         NdefFragment.setAnswer("read tag failed\n" + e.toString(), false, NdefFragment.Dart.READ_LOST);
         Map<String, String> map = new HashMap<>();
         map.put("error", e.toString());
         if (result != null)
            result.success(map);
      }
   }

   /**
    * Reads/writes Config.
    *
    * @throws IOException     d
    * @throws FormatException d
    */
   public void readWriteConfigRegister() throws CommandNotSupportedException {
      // Check if the operation is read or write
      if (RegisterConfigActivity.isWriteChosen()) {
         try {
            Ntag_Get_Version.Prod prod = reader.getProduct();
            if ((prod.equals(Ntag_Get_Version.Prod.NTAG_I2C_1k_Plus)
                    || prod.equals(Ntag_Get_Version.Prod.NTAG_I2C_2k_Plus))
                    && (RegisterConfigActivity.getAuth0() & 0xFF) <= 0xEB) {
               showAuthWriteConfigAlert();
            } else {
               writeConfigRegisters();
            }
         } catch (IOException e) {
            e.printStackTrace();
            SharedCodes.Companion.log("NDemo", e.toString());

         }
      } //END if get chosen
      else {
         try {
            byte[] configRegisters = reader.getConfigRegisters();

            Ntag_I2C_Registers answer = getRegister_Settings(configRegisters);
            RegisterConfigActivity.setAnswer(answer);
            RegisterConfigActivity.setNC_Reg(configRegisters[CR_Offset.NC_REG.getValue()]);
            RegisterConfigActivity.setLD_Reg(configRegisters[CR_Offset.LAST_NDEF_PAGE.getValue()]);
            RegisterConfigActivity.setSM_Reg(configRegisters[CR_Offset.SM_REG.getValue()]);
            RegisterConfigActivity.setNS_Reg(configRegisters[CR_Offset.REG_LOCK.getValue()]);
            RegisterConfigActivity.setWD_LS_Reg(configRegisters[CR_Offset.WDT_LS.getValue()]);
            RegisterConfigActivity.setWD_MS_Reg(configRegisters[CR_Offset.WDT_MS.getValue()]);
            RegisterConfigActivity.setI2C_CLOCK_STR(configRegisters[CR_Offset.I2C_CLOCK_STR.getValue()]);

            Ntag_Get_Version.Prod prod = reader.getProduct();
            if (prod.equals(Ntag_Get_Version.Prod.NTAG_I2C_1k_Plus)
                    || prod.equals(Ntag_Get_Version.Prod.NTAG_I2C_2k_Plus)) {
               byte[] auth0Register = reader.getAuth0Register();
               byte[] accessRegister = reader.getAccessRegister();
               byte[] pti2cRegister = reader.getPTI2CRegister();

               Ntag_I2C_Plus_Registers answerPlus = getPlusAuth_Settings(auth0Register, accessRegister, pti2cRegister);
               RegisterConfigActivity.setAnswerPlus(answerPlus);
               RegisterConfigActivity.setAuth0(auth0Register[3]);
               RegisterConfigActivity.setAccess(accessRegister[0]);
               RegisterConfigActivity.setPtI2C(pti2cRegister[0]);
            }
            //grodianknot
            RegisterConfigActivity.updateToDart();
            toastText(main, "read tag successfully done",
                    Toast.LENGTH_LONG);
         } catch (CommandNotSupportedException e) {
            e.printStackTrace();
            SharedCodes.Companion.log("NDemo", e.toString());

            throw e;
         } catch (Exception e) {
            e.printStackTrace();
            SharedCodes.Companion.log("NDemo", e.toString());

            toastText(main, "read tag failed\n" + e.toString(), Toast.LENGTH_LONG);
         }
      }
   }

   /**
    * Write config registers.
    */
   private void writeConfigRegisters() {
      try {
         byte NC_Reg = (byte) RegisterConfigActivity.getNC_Reg();
         byte LD_Reg = (byte) RegisterConfigActivity.getLD_Reg();
         byte SM_Reg = (byte) RegisterConfigActivity.getSM_Reg();
         byte WD_LS_Reg = (byte) RegisterConfigActivity.getWD_LS_Reg();
         byte WD_MS_Reg = (byte) RegisterConfigActivity.getWD_MS_Reg();
         byte I2C_CLOCK_STR = (byte) RegisterConfigActivity.getI2C_CLOCK_STR();
         reader.writeConfigRegisters(NC_Reg, LD_Reg, SM_Reg, WD_LS_Reg, WD_MS_Reg, I2C_CLOCK_STR);
         Ntag_Get_Version.Prod prod = reader.getProduct();
         if (prod.equals(Ntag_Get_Version.Prod.NTAG_I2C_1k_Plus)
                 || prod.equals(Ntag_Get_Version.Prod.NTAG_I2C_2k_Plus)) {
            byte AUTH0 = (byte) RegisterConfigActivity.getAuth0();
            byte ACCESS = (byte) RegisterConfigActivity.getAccess();
            byte PT_I2C = (byte) RegisterConfigActivity.getPtI2C();
            reader.writeAuthRegisters(AUTH0, ACCESS, PT_I2C);
         }
         toastText(main, "write tag successfully done", Toast.LENGTH_LONG);
      } catch (Exception e) {
         e.printStackTrace();
         SharedCodes.Companion.log("NDemo", e.toString());

         toastText(main, "write tag failed", Toast.LENGTH_LONG);
      }
   }

   /**
    * Show authification alert dialog.
    */
   private void showAuthWriteConfigAlert() {
      // Use the Builder class for convenient dialog construction
      if (onShowAuthWriteConfigAlertHandler == null) {
         AlertDialog.Builder builder = new AlertDialog.Builder(main);
         builder.setTitle(main.getString(R.string.Dialog_enable_auth_title));
         builder.setMessage(main.getString(R.string.Dialog_enable_auth_msg));

         builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int index) {
               // Write config registers
               writeConfigRegisters();
               dialog.dismiss();
            }
         });
         builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
               dialog.dismiss();
            }
         });

         // Create the AlertDialog object and return it
         builder.create();
         builder.show();
      } else {
         onShowAuthWriteConfigAlertHandler.onShowOkDialogueForAction(
                 main.getString(R.string.Dialog_enable_auth_title),
                 main.getString(R.string.Dialog_enable_auth_msg),
                 this,
                 new Function1<Boolean, Unit>() {
                    @Override
                    public Unit invoke(Boolean b) {
                       if (b) {
                          Ntag_I2C_Demo.this.writeConfigRegisters();
                       }
                       return Unit.INSTANCE;
                    }
                 }
         );
      }
   }

   /**
    * Reads the whole tag memory content.
    *
    * @return Boolean indicating success or error
    */
   public byte[] readTagContent() {
      byte[] bytes = null;
      try {
         // The user memory and the first four pages are displayed
         int memSize = reader.getProduct().getMemsize() + 16;
         // Read all the pages using the fast read method
         bytes = reader.readEEPROM(0, memSize / reader.getBlockSize());
      } catch (IOException e) {
         e.printStackTrace();
         SharedCodes.Companion.log("NDemo", e.toString());

      } catch (FormatException e) {
         e.printStackTrace();
         SharedCodes.Companion.log("NDemo", e.toString());

      } catch (CommandNotSupportedException e) {
         e.printStackTrace();
         SharedCodes.Companion.log("NDemo", e.toString());

         showDemoNotSupportedAlert();
      } catch (Exception e) {
         e.printStackTrace();
         SharedCodes.Companion.log("NDemo", e.toString());

      }
      return bytes;
   }

   private void showTagNotPlusAlert() {
      String message = main.getString(R.string.tag_not_supported);
      String title = main.getString(R.string.tag_not_supported_title);
      showAlert(message, title);
   }

   private void showDemoNotSupportedAlert() {
      String message = main.getString(R.string.demo_not_supported);
      String title = main.getString(R.string.demo_not_supported_title);
      showAlert(message, title);
   }

   /**
    * Resets the whole tag memory content (Memory to 00000...)
    *
    * @return Boolean indicating success or error
    */
   public boolean resetTagContent() {
      boolean success = true;
      try {
         byte[] d = new byte[reader.getProduct().getMemsize()];
         //byte[] d = new byte[Ntag_I2C_Commands.firstSectorMemsize];
         reader.writeEEPROM(d, this);
      } catch (IOException e) {
         success = false;
         e.printStackTrace();
         SharedCodes.Companion.log("NDemo", e.toString());

      } catch (FormatException e) {
         success = false;
         e.printStackTrace();
         SharedCodes.Companion.log("NDemo", e.toString());

      } catch (CommandNotSupportedException e) {
         showDemoNotSupportedAlert();
         e.printStackTrace();
         SharedCodes.Companion.log("NDemo", e.toString());

      }
      return success;
   }

   /**
    * Resets the whole tag memory content (Memory to 00000...)
    *
    * @return Boolean indicating success or error
    */
   public boolean tryResetTagContent() {
      boolean success = true;
      try {
         byte[] d = new byte[(0xE1 - 0x4)];
         //byte[] d = new byte[Ntag_I2C_Commands.firstSectorMemsize];
         reader.writeEEPROM(d, this);
         NdefFragment.setAnswer(
                 "reset success",
                 true, NdefFragment.Dart.WRITE_SUCCESS);
      } catch (IOException e) {
         success = false;
         e.printStackTrace();
         SharedCodes.Companion.log("NDemo", e.toString());

      } catch (FormatException e) {
         success = false;
         e.printStackTrace();
         SharedCodes.Companion.log("NDemo", e.toString());

      } catch (CommandNotSupportedException e) {
         showDemoNotSupportedAlert();
         e.printStackTrace();
         SharedCodes.Companion.log("NDemo", e.toString());

      }
      return success;
   }

   /**
    * Performs the LED Demo
    */
   public void LED() throws IOException, FormatException {
      // Reset UI
      LedFragment.setAnswer(main.getResources().getString(R.string.readConf));
      LedFragment.setTemperatureC(0);
      LedFragment.setTemperatureF(0);
      LedFragment.setVoltage(0);

      // The demo is executed in a separate thread to let the GUI run
      lTask = new LedTask();
      lTask.execute();
   }

   //fixme:
   private class LedTask extends AsyncTask<Void, Byte[], Void> {
      private final byte deviceToTag = 1;
      private final byte tagToDevice = 2;
      private final byte noTransfer = 0;
      private final byte invalidTransfer = 4;

      private Boolean exit = false;

      @Override
      protected Void doInBackground(Void... params) {
         byte[] dataTx = new byte[reader.getSRAMSize()];
         byte[] dataRx = new byte[reader.getSRAMSize()];
         byte[] Led;
         Byte[][] result;

         // We have to make sure that the Pass-Through mode is activated
         long RegTimeOutStart = System.currentTimeMillis();
         boolean RTest = false;
         try {
            do {
               if (reader.checkPTwritePossible()) {
                  break;
               }
               long RegTimeOut = System.currentTimeMillis();
               RegTimeOut = RegTimeOut - RegTimeOutStart;
               RTest = (RegTimeOut < 5000);
            } while (RTest);
            // Do as long as no Exception is thrown
            while (true) {
               // Get the color to be transmitted
               Led = LedFragment.getOption().getBytes();

               // Write the color into the block to be transmitted to the
               // NTAG board
               dataTx[reader.getSRAMSize() - 4] = Led[0];
               dataTx[reader.getSRAMSize() - 3] = Led[1];

               // Indicate whether Temperate and LCD are enabled or not
               if (LedFragment.isTempEnabled()) {
                  dataTx[reader.getSRAMSize() - 9] = 'E';
               } else {
                  dataTx[reader.getSRAMSize() - 9] = 0x00;
               }
               if (LedFragment.isLCDEnabled()) {
                  dataTx[reader.getSRAMSize() - 10] = 'E';
               } else {
                  dataTx[reader.getSRAMSize() - 10] = 0x00;
               }
               // NDEF Scrolling activation
               if (LedFragment.isScrollEnabled()) {
                  dataTx[reader.getSRAMSize() - 11] = 'E';
               } else {
                  dataTx[reader.getSRAMSize() - 11] = 0x00;
               }
               double tempC = LedFragment.getTemperatureC();
               double tempF = LedFragment.getTemperatureF();
               if (tempC > 0.0 && tempC < 75.0) {
                  DecimalFormat df = new DecimalFormat("00.00");
                  byte[] tempB = df.format(tempC).getBytes();

                  // The '.' is omitted
                  dataTx[reader.getSRAMSize() - 24] = tempB[0];
                  dataTx[reader.getSRAMSize() - 23] = tempB[1];
                  dataTx[reader.getSRAMSize() - 22] = tempB[3];
                  dataTx[reader.getSRAMSize() - 21] = tempB[4];
               }
               if (tempF > 0.0 && tempF < 120.0) {
                  DecimalFormat df = new DecimalFormat("000.00");
                  byte[] tempB = df.format(tempF).getBytes();

                  // The '.' is omitted
                  dataTx[reader.getSRAMSize() - 19] = tempB[0];
                  dataTx[reader.getSRAMSize() - 18] = tempB[1];
                  dataTx[reader.getSRAMSize() - 17] = tempB[2];
                  dataTx[reader.getSRAMSize() - 16] = tempB[4];
                  dataTx[reader.getSRAMSize() - 15] = tempB[5];
               }
               double voltD = LedFragment.getVoltage();

               if (voltD > 0.0 && voltD < 5.0) {
                  DecimalFormat df = new DecimalFormat("0.0");
                  byte[] voltB = df.format(voltD).getBytes();

                  // The '.' is omitted
                  dataTx[reader.getSRAMSize() - 8] = voltB[0];
                  dataTx[reader.getSRAMSize() - 7] = voltB[2];
               }
               displayTransferDir(deviceToTag);

               // wait to prevent that a RF communication is
               // at the same time as ??C I2C
               Thread.sleep(10);
               reader.waitforI2Cread(DELAY_TIME);
               reader.writeSRAMBlock(dataTx, null);
               displayTransferDir(tagToDevice);

               // wait to prevent that a RF communication is
               // at the same time as ??C I2C
               Thread.sleep(10);

               reader.waitforI2Cwrite(100);

               dataRx = reader.readSRAMBlock();

               if (exit) {
                  // switch off the LED on the ??C before terminating

                  dataTx[reader.getSRAMSize() - 4] = Led[0];
                  dataTx[reader.getSRAMSize() - 3] = '0';

                  // wait to prevent that a RF communication is
                  // at the same time as ??C I2C
                  Thread.sleep(10);
                  reader.waitforI2Cread(100);

                  reader.writeSRAMBlock(dataTx, null);

                  // wait to prevent that a RF communication is
                  // at the same time as ??C I2C
                  Thread.sleep(10);
                  reader.waitforI2Cwrite(100);

                  dataRx = reader.readSRAMBlock();

                  cancel(true);
                  return null;
               }

               // Convert byte[] to Byte[]
               Byte[] bytes = new Byte[dataRx.length];
               for (int i = 0; i < dataRx.length; i++) {
                  bytes[i] = dataRx[i];
               }
               result = new Byte[2][];
               result[0] = new Byte[1];
               result[0][0] = (byte) invalidTransfer;
               result[1] = bytes;

               // Write the result to the UI thread
               publishProgress(result);

            }
         } catch (FormatException e) {
            displayTransferDir(noTransfer);
            cancel(true);
            e.printStackTrace();
            SharedCodes.Companion.log("NDemo", e.toString());

         } catch (IOException e) {
            displayTransferDir(noTransfer);
            cancel(true);
            e.printStackTrace();
            SharedCodes.Companion.log("NDemo", e.toString());

         } catch (CommandNotSupportedException e) {
            showDemoNotSupportedAlert();
            displayTransferDir(noTransfer);
            cancel(true);
            e.printStackTrace();
            SharedCodes.Companion.log("NDemo", e.toString());

         } catch (Exception e) {
            displayTransferDir(noTransfer);
            cancel(true);
            e.printStackTrace();
            SharedCodes.Companion.log("NDemo", e.toString());

         }
         return null;
      }

      private void displayTransferDir(byte dir) {
         Byte[][] result;
         result = new Byte[2][];
         result[0] = new Byte[1];
         result[0][0] = (byte) dir;
         publishProgress(result);
      }

      @Override
      protected void onProgressUpdate(Byte[]... bytes) {
         if (bytes[0][0] == noTransfer) {
            LedFragment.setTransferDir("Transfer: non");
         } else if (bytes[0][0] == deviceToTag) {
            LedFragment.setTransferDir("Transfer: Device --> Tag");
         } else if (bytes[0][0] == tagToDevice) {
            LedFragment.setTransferDir("Transfer: Device <-- Tag");
         } else {
            LedFragment.setButton(bytes[1][reader.getSRAMSize() - 2]);

            int temp = 0;

            // Adding first "Byte"
            temp = ((bytes[1][reader.getSRAMSize() - 5] >> 5) & 0x00000007);

            // Adding second Byte
            temp |= ((bytes[1][reader.getSRAMSize() - 6] << 3) & 0x000007F8);

            // Voltage
            int voltage = 0;
            voltage = ((bytes[1][reader.getSRAMSize() - 7] << 8) & 0xff00)
                    + (bytes[1][reader.getSRAMSize() - 8] & 0x00ff);

            // if Temp is 0 no Temp sensor is on the ??C
            if (temp != 0) {
               // Set the values on the screen
               LedFragment.setAnswer("Temperature: "
                       + calcTempCelsius(temp) + " ??C / "
                       + calcTempFarenheit(temp) + " ??F"
                       + "\nEnergy Harvesting Voltage: "
                       + calcVoltage(voltage));
            } else {
               LedFragment.setAnswer("Temperature: " + "Not available"
                       + "\nEnergy Harvesting Voltage: "
                       + calcVoltage(voltage));
               LedFragment.setTemperatureC(0);
               LedFragment.setTemperatureF(0);
            }

            byte version = bytes[1][reader.getSRAMSize() - 1];
            if (version > 0) {
               int uppVersion = (version >> 4) & 0x0f;
               int lowVersion = version & 0x0f;

               PseudoMainActivity.setBoardFirmwareVersion(String
                       .valueOf(uppVersion)
                       + "."
                       + String.valueOf(lowVersion));
            } else {
               PseudoMainActivity.setBoardFirmwareVersion("1.0");
            }
         }
         // gordianknot
         LedFragment.updateToUi();
      }
   }

   /**
    * Stops the LED Demo.
    */
   public void LEDFinish() {
      if (lTask != null && !lTask.isCancelled()) {
         lTask.exit = true;
         try {
            lTask.get();
         } catch (Exception e) {
            e.printStackTrace();
            SharedCodes.Companion.log("NDemo", e.toString());

         }
         lTask = null;
      }
   }

   /**
    * Stops the NDEFRead Demo
    */
   public void NDEFReadFinish() {
      if (ndefreadtask != null && !ndefreadtask.isCancelled()) {
         ndefreadtask.exit = true;
         try {
            ndefreadtask.get();
         } catch (Exception e) {
            e.printStackTrace();
            SharedCodes.Companion.log("NDemo", e.toString());

         }
         ndefreadtask = null;
         // Clean all the fields
         NdefFragment.resetNdefDemo();
      }
   }

   public NdefMessage getMessageByType() throws IOException {
      NdefMessage msg = null;
      // Get the selected NDEF type since the creation of the NDEF Msg
      // will vary depending on the type
      if (NdefFragment.isRadioText()) {
         msg = createNdefTextMessage(NdefFragment.getText());
      } else if (NdefFragment.isRadioBytes()) {
         msg = createNdefBytesMessage(NdefFragment.getBytes());
      } else if (NdefFragment.isRadioUrl()) {
         msg = createNdefUriMessage(NdefFragment.getText());
      } else if (NdefFragment.isRadioBt()) {
         msg = createNdefBSSPMessage();
      } else if (NdefFragment.isRadioSp()) {
         msg = createNdefSpMessage(
                 NdefFragment.getSpTitle(),
                 NdefFragment.getSpLink());
      }
      if (msg == null) {
         //gordianknot - uncomment to recover
         //toastText(main, "Please add correct input values", Toast.LENGTH_LONG) ;
         NdefFragment.setAnswer(main.getResources().getString(R.string.format_error),
                 true, NdefFragment.Dart.INCORRECT_CONTENT);
      }
      return msg;
   }

   /**
    * Performs the NDEF Demo
    */
   // note: never used
   public void NDEF() throws IOException {
      Boolean custom = SharedCodes.Companion.getCustomWrite();
      // Check if the operation is read or write
      if (NdefFragment.isWriteChosen()) {
         if (custom)
            SharedCodes.Companion.log("writeNDEFCustom", "NDEF()..." + custom);
         else
            SharedCodes.Companion.log("writeNDEFNormal", "NDEF()..." + custom);

         // NDEF Message to write in the tag
         NdefMessage msg = getMessageByType();

         if (NdefFragment.isAarRecordSelected()) {
            NdefRecord aarRecord = NdefRecord.createApplicationRecord(PseudoMainActivity.getPACKAGE_NAME());
            assert msg != null;
            NdefRecord records[] = msg.getRecords();
            records = Arrays.copyOf(records, records.length + 1);
            records[records.length - 1] = aarRecord;
            msg = new NdefMessage(records);
         }
         try {
            long timeToWriteNdef;
            if (custom) {
               timeToWriteNdef = NDEFWriteCustom(msg);
            } else {
               timeToWriteNdef = NDEFWrite(msg);
            }

            // Not needed for now
            // Inform the MCU about the new NDEF message that has been
            // stored
            // N: reference for NDEF
//				if (!(reader instanceof MinimalNtag_I2C_Commands)
//						&& reader.checkPTwritePossible()) {
//					byte[] Data = new byte[64];
//					Data[reader.getSRAMSize() - 4] = 'N';
//					reader.writeSRAMBlock(Data, null);
//				}
            // gordianknot - uncomment to recover
            //toastText(main, "write tag successfully done", Toast.LENGTH_LONG) ;
            NdefFragment.setAnswer("Tag successfully written", false, NdefFragment.Dart.WRITE_SUCCESS);
            assert msg != null;
            int bytes = msg.toByteArray().length;

            String Message = "";

            // Transmission Results
            Message = Message.concat("Speed (" + bytes + " Byte / "
                    + timeToWriteNdef + " ms): "
                    + String.format("%.0f", bytes / (timeToWriteNdef / 1000.0))
                    + " Bytes/s");
            NdefFragment.setDatarate(Message);
         } catch (Exception e) {
            //gordianknot - uncomment to recover
            //toastText(main, "write tag failed", Toast.LENGTH_LONG) ;
            NdefFragment.setDatarate("Error writing NDEF");
            NdefFragment.setAnswer("Error writing NDEF", true, NdefFragment.Dart.WRITE_ERROR);
            e.printStackTrace();
            SharedCodes.Companion.log("NDemo", e.toString());
         }
      } else {
         if (custom) SharedCodes.Companion.log("readNDEFCustom", "NDEF()...");
         else        SharedCodes.Companion.log("readNDEFNormal", "NDEF()...");

         ndefreadtask = new NDEFReadTask();
         ndefreadtask.execute();
      }
   }




   public void NDEFCustom(byte startAt, byte[] bytes, int length, MethodChannel.Result result) throws IOException {
      Boolean custom = SharedCodes.Companion.getCustomWrite();
      // Check if the operation is read or write
      if (NdefFragment.isWriteChosen()) {
         SharedCodes.Companion.log("NDEFCustom-write", "bytes:" + Arrays.toString(bytes));
         // NDEF Message to write in the tag
         NdefMessage msg = createNdefBytesMessage(bytes);
         try {
            reader.writeNDEFCustom(msg, this);
            // gordianknot - uncomment to recover
            //toastText(main, "write tag successfully done", Toast.LENGTH_LONG) ;
            NdefFragment.setAnswer("Tag successfully written", false, NdefFragment.Dart.WRITE_SUCCESS);
            result.success(true);

         } catch (Exception e) {
            //gordianknot - uncomment to recover
            NdefFragment.setAnswer("Error writing NDEF", true,
                    NdefFragment.Dart.WRITE_ERROR);
            e.printStackTrace();
            SharedCodes.Companion.log("NDemo", e.toString());
         }
      } else {
         try {
            SharedCodes.Companion.log("NDEFCustom-read", "bytes:" + Arrays.toString(bytes));
            readSessionRegistersCustom(startAt, length, result);
         } catch (CommandNotSupportedException e) {
            NdefFragment.setAnswer("read register failed", false, NdefFragment.Dart.READ_LOST);
         }
         /*try{
            SharedCodes.Companion.log("NDEFCustom-read", "NDEF()...");
            NtagRawRecord record = reader.readNDEFCustom();
            NdefFragment.setNdefType("none");
            NdefFragment.setNdefMessage("");
            NdefFragment.setDatarate("");
            NdefFragment.setRawRecord(record);
            result.success(NdefFragment.asMap());
            NdefFragment.setAnswer("read success", false, NdefFragment.Dart.READ_SUCCESS);
            NdefFragment.resetNdefDemo();

         }catch (CommandNotSupportedException e) {
            e.printStackTrace();
            SharedCodes.Companion.log("NDemo", e.toString());
            showAlert("The NDEF Message is to long to read from an NTAG I2C 2K with this Nfc Device",
                    "Demo not supported");
            NdefFragment.setAnswer(
                    "The NDEF Message is to long to read from an NTAG I2C 2K with this Nfc Device",
                    true, NdefFragment.Dart.READ_ERROR);
         } catch (Exception e) {
            e.printStackTrace();
            SharedCodes.Companion.log("NDemo", e.toString());
            NdefFragment.setAnswer(e.toString(), true, NdefFragment.Dart.READ_ERROR);
         }*/
      }
   }

   // note: never used
   private class NDEFReadTask extends AsyncTask<Void, String, Void> {
      private Boolean exit = false;
      private NtagRawRecord record;

      @Override
      protected Void doInBackground(Void... params) {
         Boolean custom = SharedCodes.Companion.getCustomWrite();
         try {
            while (true) {

               // cancle task if requested
               if (exit) {
                  cancel(true);
                  return null;
               }
               long RegTimeOutStart = System.currentTimeMillis();

               // Get the message Type
               NdefMessage msg;
               if (custom) {
                  this.record = reader.readNDEFCustom();
                  msg = record.getMessage();
               } else {
                  msg = reader.readNDEF();
               }

               if (msg != null){
                  // NDEF Reading time statistics
                  long timeToReadNdef = System.currentTimeMillis() - RegTimeOutStart;
                  Message highLevelMsg = new Message(msg);

                  // Get the message type in order to deal with it
                  // appropriately
                  String type = "none";
                  if (!highLevelMsg.isEmpty()) {
                     type = highLevelMsg.get(0).getClass().getSimpleName();
                  }

                  String message = "";
                  int num_record = 1;

                  for (Record rec : highLevelMsg) {
                     if (rec instanceof EmptyRecord) {
                        message += "#" + num_record++ + ": "
                                + "EmptyRecord\n";
                     } else if (rec instanceof SmartPosterRecord) {
                        message += "#" + num_record + " "
                                + rec.getClass().getSimpleName() + ":\n";
                        SmartPosterRecord smp = (SmartPosterRecord) rec;
                        message += "#" + num_record + ".1 "
                                + smp.getTitle().getClass().getSimpleName()
                                + ":\n" + smp.getTitle().getText() + "\n";
                        message += "#" + num_record + ".2 "
                                + smp.getUri().getClass().getSimpleName()
                                + ":\n" + smp.getUri().getUri().getHost()
                                + smp.getUri().getUri().getPath() + "\n\n";
                        num_record++;
                     } else if (rec instanceof TextRecord) {
                        //gordianknot..
                        NdefFragment.setNdefMessage(((TextRecord) rec).getNdefRecord().getPayload().clone());
                        message += "#" + num_record++ + " "
                                + rec.getClass().getSimpleName() + ":\n"
                                + ((TextRecord) rec).getText() + "\n\n";
                     } else if (rec instanceof AndroidApplicationRecord) {
                        message += "#"
                                + num_record++
                                + " "
                                + rec.getClass().getSimpleName()
                                + ":\n"
                                + ((AndroidApplicationRecord) rec)
                                .getPackageName() + "\n\n";
                     } else if (rec instanceof UriRecord) {
                        message += "#" + num_record++ + " "
                                + rec.getClass().getSimpleName() + ":\n"
                                + ((UriRecord) rec).getUri().getHost()
                                + ((UriRecord) rec).getUri().getPath()
                                + "\n\n";
                     } else if (rec instanceof MimeRecord) {
                        message += "#" + num_record++ + " "
                                + rec.getClass().getSimpleName() + ":\n"
                                + ((MimeRecord) rec).getMimeType() + "\n\n";
                     } else {
                        message += "#" + num_record++ + " "
                                + rec.getClass().getSimpleName() + "\n\n";
                     }
                  }
                  int bytes = msg.toByteArray().length;
                  String readTimeMessage = "";

                  // Transmission Results
                  readTimeMessage = readTimeMessage.concat("Speed (" + bytes + " Byte / "
                          + timeToReadNdef + " ms): "
                          + String.format("%.0f", bytes / (timeToReadNdef / 1000.0))
                          + " Bytes/s");

                  // Put the message content on the screen
                  publishProgress(type, message, readTimeMessage);
                  // sleep 500ms, but check if task was cancelled
                  for (int i = 0; i < 5; i++) {
                     // cancle task if requested
                     if (exit || !NdefFragment.isNdefReadLoopSelected()) {
                        cancel(true);
                        return null;
                     }
                     Thread.sleep(DELAY_TIME);
                  }

                  // Don't let the thread repeat
                  Thread.sleep(DELAY_TIME);
               }else{
                  // Put the message content on the screen
                  publishProgress("", "", "");
                  // sleep 500ms, but check if task was cancelled
                  for (int i = 0; i < 5; i++) {
                     // cancle task if requested
                     if (exit || !NdefFragment.isNdefReadLoopSelected()) {
                        cancel(true);
                        return null;
                     }
                     Thread.sleep(DELAY_TIME);
                  }

                  // Don't let the thread repeat
                  Thread.sleep(DELAY_TIME);
               }
            }
         } catch (CommandNotSupportedException e) {
            e.printStackTrace();
            SharedCodes.Companion.log("NDemo", e.toString());

            showAlert("The NDEF Message is to long to read from an NTAG I2C 2K with this Nfc Device",
                    "Demo not supported");
            NdefFragment.setAnswer(
                    "The NDEF Message is to long to read from an NTAG I2C 2K with this Nfc Device",
                    true, NdefFragment.Dart.READ_ERROR);
         } catch (Exception e) {
            e.printStackTrace();
            SharedCodes.Companion.log("NDemo", e.toString());

            NdefFragment.setAnswer(e.toString(), true, NdefFragment.Dart.READ_ERROR);
         }
         return null;
      }

      @Override
      protected void onCancelled() {
         SharedCodes.Companion.log("NdeFrag", "onCancelled");
         NdefFragment.updateToUi();
      }

      @Override
      protected void onProgressUpdate(String... progress) {
         NdefFragment.setNdefType(progress[0]);
         NdefFragment.setNdefMessage(progress[1]);
         NdefFragment.setDatarate(progress[2]);
         NdefFragment.setRawRecord(record);
			/*NdefFragment.updateProgress(
				progress[2], progress[1], progress[0]
			);*/
      }

      @Override
      protected void onPostExecute(Void bla) {
         if (!exit) {
            toastText(main, main.getString(R.string.Tag_lost),
                    Toast.LENGTH_LONG);
         }
         SharedCodes.Companion.log("NdeFrag", "onPostExecute");
         NdefFragment.updateToUi();
         NdefFragment.resetNdefDemo();
      }
   }

   /**
    * Performs the flashing of a new bin file from the NFC Device.
    *
    * @param bytesToFlash Byte Array containing the new firmware to be flashed
    * @return Boolean operation result
    * @throws IOException
    * @throws FormatException
    */
   public Boolean Flash(byte[] bytesToFlash) {
      int sectorSize = PAGE_SIZE;

      byte[] data = null;
      byte[] flashData = null;

      try {
         int length = bytesToFlash.length;
         int flashes = length / sectorSize + (length % sectorSize == 0 ? 0 : 1);
         int blocks = (int) Math.ceil(length / (float) reader.getSRAMSize());

         // Set the number of writings
         FlashMemoryActivity.setFLashDialogMax(blocks);

         for (int i = 0; i < flashes; i++) {
            int flash_addr = 0x4000 + i * sectorSize;
            int flash_length = 0;

            if (length - (i + 1) * sectorSize < 0) {
               flash_length = roundUp(length % sectorSize);
               flashData = new byte[flash_length];
               Arrays.fill(flashData, (byte) 0);
               System.arraycopy(bytesToFlash, i * sectorSize, flashData, 0, length % sectorSize);
            } else {
               flash_length = sectorSize;
               flashData = new byte[flash_length];
               System.arraycopy(bytesToFlash, i * sectorSize, flashData, 0, sectorSize);
            }

            data = new byte[reader.getSRAMSize()];
            data[reader.getSRAMSize() - 4] = 'F';
            data[reader.getSRAMSize() - 3] = 'P';

            data[reader.getSRAMSize() - 8] = (byte) (flash_length >> 24 & 0xFF);
            data[reader.getSRAMSize() - 7] = (byte) (flash_length >> 16 & 0xFF);
            data[reader.getSRAMSize() - 6] = (byte) (flash_length >> 8 & 0xFF);
            data[reader.getSRAMSize() - 5] = (byte) (flash_length & 0xFF);

            data[reader.getSRAMSize() - 12] = (byte) (flash_addr >> 24 & 0xFF);
            data[reader.getSRAMSize() - 11] = (byte) (flash_addr >> 16 & 0xFF);
            data[reader.getSRAMSize() - 10] = (byte) (flash_addr >> 8 & 0xFF);
            data[reader.getSRAMSize() - 9] = (byte) (flash_addr & 0xFF);

            SharedCodes.Companion.log("FLASH", "Flashing to start");
            reader.writeSRAMBlock(data, null);
            SharedCodes.Companion.log("FLASH", "Start Block write " + (i + 1) + " out of " + flashes);

            reader.waitforI2Cread(100);

            SharedCodes.Companion.log("FLASH", "Starting Block writing");
            reader.writeSRAM(flashData, R_W_Methods.Fast_Mode, this);
            SharedCodes.Companion.log("FLASH", "All Blocks written");

            reader.waitforI2Cwrite(500);
            Thread.sleep(500);

            SharedCodes.Companion.log("FLASH", "Wait finished");
            byte[] response = reader.readSRAMBlock();
            SharedCodes.Companion.log("FLASH", "Block read");

            if (response[reader.getSRAMSize() - 4] != 'A' || response[reader.getSRAMSize() - 3] != 'C' || response[reader.getSRAMSize() - 2] != 'K') {
               SharedCodes.Companion.log("FLASH", "was nak");
               return false;
            }
            SharedCodes.Companion.log("FLASH", "was ack");
         }
         SharedCodes.Companion.log("FLASH", "Flash completed");

         data = new byte[reader.getSRAMSize()];
         data[reader.getSRAMSize() - 4] = 'F';
         data[reader.getSRAMSize() - 3] = 'S';
         reader.writeSRAMBlock(data, null);

         // Wait for the I2C to be ready
         reader.waitforI2Cread(DELAY_TIME);
         return true;
      } catch (IOException e) {
         e.printStackTrace();
         SharedCodes.Companion.log("NDemo", e.toString());

      } catch (FormatException e) {
         e.printStackTrace();
         SharedCodes.Companion.log("NDemo", e.toString());

      } catch (TimeoutException e) {
         e.printStackTrace();
         SharedCodes.Companion.log("NDemo", e.toString());

      } catch (CommandNotSupportedException e) {
         e.printStackTrace();
         SharedCodes.Companion.log("NDemo", e.toString());

      } catch (InterruptedException e) {
         e.printStackTrace();
         SharedCodes.Companion.log("NDemo", e.toString());

      }
      SharedCodes.Companion.log("FLASH", "Flash returned");

      data = new byte[reader.getSRAMSize()];
      data[reader.getSRAMSize() - 4] = 'F';
      data[reader.getSRAMSize() - 3] = 'F';

      try {
         reader.writeSRAMBlock(data, null);

         // Wait for the I2C to be ready
         reader.waitforI2Cread(100);
      } catch (IOException e) {
         e.printStackTrace();
         SharedCodes.Companion.log("NDemo", e.toString());

      } catch (FormatException e) {
         e.printStackTrace();
         SharedCodes.Companion.log("NDemo", e.toString());

      } catch (CommandNotSupportedException e) {
         e.printStackTrace();
         SharedCodes.Companion.log("NDemo", e.toString());

      } catch (TimeoutException e) {
         e.printStackTrace();
         SharedCodes.Companion.log("NDemo", e.toString());

      }
      return false;
   }

   /**
    * Retrieves the auth status of the tag
    *
    * @return int current auth status
    * @throws IOException
    */
   public int ObtainAuthStatus() {
      try {
         Ntag_Get_Version.Prod prod = reader.getProduct();
         if (!prod.equals(Ntag_Get_Version.Prod.NTAG_I2C_1k_Plus)
                 && !prod.equals(Ntag_Get_Version.Prod.NTAG_I2C_2k_Plus)) {
            return AuthStatus.Disabled.getValue();
         } else {
            return reader.getProtectionPlus();
         }
      } catch (IOException e) {
         e.printStackTrace();
         SharedCodes.Companion.log("NDemo", e.toString());

      }
      return AuthStatus.Disabled.getValue();
   }

   /**
    * Performs the authentication operation on NTAG I2C Plus
    *
    * @param pwd        Byte Array containing the password
    * @param authStatus Current Authentication Status
    * @return Boolean operation result
    * @throws IOException
    * @throws FormatException
    */
   public Boolean Auth(byte[] pwd, int authStatus) {
      try {
         if (authStatus == AuthStatus.Unprotected.getValue()) {
            reader.protectPlus(pwd, Register.Capability_Container.getValue());
         } else if (authStatus == AuthStatus.Authenticated.getValue()) {
            reader.unprotectPlus();
         } else if (authStatus == AuthStatus.Protected_W.getValue()
                 || authStatus == AuthStatus.Protected_RW.getValue()
                 || authStatus == AuthStatus.Protected_W_SRAM.getValue()
                 || authStatus == AuthStatus.Protected_RW_SRAM.getValue()) {
            byte[] pack = reader.authenticatePlus(pwd);
            if (pack.length < 2) {
               return false;
            }
         }
         return true;
      } catch (IOException e) {
         e.printStackTrace();
         SharedCodes.Companion.log("NDemo", e.toString());

      } catch (FormatException e) {
         e.printStackTrace();
         SharedCodes.Companion.log("NDemo", e.toString());

      } catch (NotPlusTagException e) {
         showTagNotPlusAlert();
         e.printStackTrace();
         SharedCodes.Companion.log("NDemo", e.toString());

      }
      return false;
   }

   /***
    * Helper method to adjusts the number of bytes to be sent during the last flashing sector
    *
    * @param num to round up
    * @return number roundep up
    */
   int roundUp(int num) {
      if (num <= 256) {
         return 256;
      } else if (num > 256 && num <= 512) {
         return 512;
      } else if (num > 512 && num <= 1024) {
         return 1024;
      } else {
         return 4096;
      }
   }

   /**
    * Performs the SRAM Speedtest
    *
    * @throws IOException
    * @throws FormatException
    */
   public void SRAMSpeedtest() throws IOException, FormatException {
      sramspeedtask = new SRAMSpeedtestTask();
      sramspeedtask.execute();
   }

   private class SRAMSpeedtestTask extends AsyncTask<Void, String, Void> {

      private Boolean exit = false;
      private byte[] Data;
      private int chMultiplier;
      private R_W_Methods method;
      private long readerTagTime;
      private long tagReadTime;
      private boolean isValidRxData = false;
      private boolean isValidTxData = false;
      private boolean isValidFirmware = false;

      @Override
      protected void onPreExecute() {
         SpeedTestFragment.setAnswer("SRAM Speedtest");

         // getting Block multiplier
         String blockMulti = SpeedTestFragment.getrf_ndef_value_charmulti();
         chMultiplier = 1;
         int chMultiLength = blockMulti.length();

         if (chMultiLength == 0) {
            chMultiplier = 1;
         } else {
            chMultiplier = Integer.parseInt(blockMulti);
         }

         // get Read Method
         if (SpeedTestFragment.getReadOptions().equalsIgnoreCase(
                 main.getString(R.string.rf_readOptions_fast_mode))) {
            method = R_W_Methods.Fast_Mode;
         } else if (SpeedTestFragment.getReadOptions().equalsIgnoreCase(
                 main.getString(R.string.rf_readOptions_polling_mode))) {
            method = R_W_Methods.Polling_Mode;
            // The minimalNtag_I2C_Commands does not use the sector_select,
            // so it cannot access the registers
            if (reader instanceof MinimalNtag_I2C_Commands) {
               //fixme:
               new AlertDialog.Builder(main)
                       .setMessage(
                               "This NFC device does not support the NFC Forum "
                                       + "commands needed to use the polling mode")
                       .setTitle("Command not supported")
                       .setPositiveButton("OK",
                               new DialogInterface.OnClickListener() {
                                  @Override
                                  public void onClick(
                                          DialogInterface dialog,
                                          int which) {
                                  }
                               }).show();
               exit = true;
            }
         } else {
            method = R_W_Methods.Error;
         }
      }

      @Override
      protected Void doInBackground(Void... params) {
         // cancle task if requested
         if (exit) {
            cancel(true);
            return null;
         }
         try {
            Ntag_Get_Version.Prod prod = reader.getProduct();
            // With some old NFC Controllers The NTAG I2C Plus might lose the authentication when coming from writing/reading SRAM
            // so it is better to reauthenticate just in case
            if (prod.equals(Ntag_Get_Version.Prod.NTAG_I2C_1k_Plus)
                    || prod.equals(Ntag_Get_Version.Prod.NTAG_I2C_2k_Plus)) {
               if (PseudoMainActivity.getAuthStatus() == AuthStatus.Authenticated.getValue()) {
                  reader.authenticatePlus(PseudoMainActivity.getPassword());
               }
            }
            long regTimeOutStart = System.currentTimeMillis();

            while (true) {
               if (reader.checkPTwritePossible()) {
                  publishProgress("Pass Through Mode is ON");
                  break;
               } else {
                  publishProgress("Pass Through Mode is OFF");
               }
               if (((System.currentTimeMillis() - regTimeOutStart) > 500)) {
                  publishProgress("Pass Through Mode was OFF, ERROR time out");
                  cancel(true);
                  return null;
               }
            }
            Data = new byte[reader.getSRAMSize()];
            Data[reader.getSRAMSize() - 4] = 'S';
            reader.writeSRAMBlock(Data, null);

            // ///////////////
            // Begin to transmit Data (RF -> I2C)
            // ///////////////

            // create array
            Data = new byte[chMultiplier * reader.getSRAMSize()];

            // adding the finish text
            int last_Block = (Data.length - reader.getSRAMSize());
            byte[] fin = "finish_S_".getBytes();
            System.arraycopy(fin, 0, Data, last_Block, 9);

            long currTime;

            // Append a CRC32 in the last block for the whole message
            Data = appendCRC32(Data);

            // write to SRAM
            currTime = System.currentTimeMillis();

            // Modified reader.writeSRAM() to also get update to the UI
            // -----
            reader.waitforI2Cread(DELAY_TIME);
            int Blocks = (int) Math.ceil(Data.length / (float) reader.getSRAMSize());
            for (int i = 0; i < Blocks; i++) {
               byte[] dataBlock = new byte[reader.getSRAMSize()];
               if (Data.length - (i + 1) * reader.getSRAMSize() < 0) {
                  Arrays.fill(dataBlock, (byte) 0);
                  System.arraycopy(Data, i * reader.getSRAMSize(),
                          dataBlock, 0, Data.length % reader.getSRAMSize());
               } else {
                  System.arraycopy(Data, i * reader.getSRAMSize(),
                          dataBlock, 0, reader.getSRAMSize());
               }
               reader.writeSRAMBlock(dataBlock, null);

               // cancle task if requested
               if (exit) {
                  // wait for the error detection on the ??C
                  Thread.sleep(550);
                  cancel(true);
                  return null;
               }

               // Publish the Progress
               publishProgress(((i + 1) * reader.SRAMSize)
                       + " Bytes written");
               if (method == R_W_Methods.Polling_Mode) {
                  reader.waitforI2Cread(DELAY_TIME);
               } else {
                  try {
                     // else wait
                     Thread.sleep(6);
                  } catch (Exception e) {
                     e.printStackTrace();
                     SharedCodes.Companion.log("NDemo", e.toString());

                  }
               }
            }
            readerTagTime = System.currentTimeMillis() - currTime;

            // force cached sector back to Sector0 to prevent from
            // hidden change of sector due to presents detection
            // reader.readEEPROM(0x04, 0x04);
            Thread.sleep(10);
            String currentDatarateCallback = SpeedTestFragment
                    .getDatarateCallback();

            // reading SRAM
            currTime = System.currentTimeMillis();

            // ///////////////
            // Begin to Read Data (I2C -> RF)
            // ///////////////

            // Modified reader.readSRAM() to also get update to the UI
            // -----
            byte[] response = new byte[0];
            byte[] temp;
            for (int i = 0; i < chMultiplier; i++) {
               if (method == R_W_Methods.Polling_Mode) {
                  reader.waitforI2Cwrite(DELAY_TIME);
               } else {
                  try {
                     // else wait
                     Thread.sleep(6);
                  } catch (Exception e) {
                     e.printStackTrace();
                     SharedCodes.Companion.log("NDemo", e.toString());

                  }
               }
               temp = reader.readSRAMBlock();

               // cancle task if requested
               if (exit) {
                  // wait for the error detection on the ??C
                  Thread.sleep(550);
                  cancel(true);
                  return null;
               }
               // Publish the Progress
               publishProgress(currentDatarateCallback + "\n"
                       + ((i + 1) * reader.SRAMSize) + " Bytes read");

               // concat read block to the full response
               response = concat(response, temp);
            }
            // -----

            // if a error is detected [reader.getSRAMSize() - 5] is set to
            // 0x01
            if (response[reader.getSRAMSize() - 5] == 0x01) {
               isValidTxData = false;
            } else {
               isValidTxData = true;
            }
            tagReadTime = System.currentTimeMillis() - currTime;
            isValidFirmware = isCRC32Appended(response);

            if (isValidFirmware) {
               isValidRxData = isValidCRC32(response);
            }
         } catch (CommandNotSupportedException e) {
            e.printStackTrace();
            SharedCodes.Companion.log("NDemo", e.toString());

            showDemoNotSupportedAlert();
            cancel(true);
            return null;
         } catch (Exception e) {
            e.printStackTrace();
            SharedCodes.Companion.log("NDemo", e.toString());


            // Inform the user about the error
//				String Message = "Phone sends too fast or Tag was lost, try Polling mode";
            String Message = "Phone sends too fast or Tag was lost";
            publishProgress(Message);
            cancel(true);
            return null;
         }
         return null;
      }

      @Override
      protected void onProgressUpdate(String... progress) {
         SpeedTestFragment.setDatarateCallback(progress[0]);
      }

      @Override
      protected void onPostExecute(Void nothing) {

         // ///////////////
         // Show Results
         // ///////////////

         // getting results
         int bytes = (chMultiplier * reader.getSRAMSize());

         String message = "";

         // Result of the Integrity check based on the calculation of the
         // CRC32
         if (isValidFirmware) {
            String rxResult = "";

            if (isValidRxData) {
               rxResult = "OK";
            } else {
               rxResult = "Error";
            }
            String txResult = "";

            if (isValidTxData) {
               txResult = "OK";
            } else {
               txResult = "Error";
            }
            message = message.concat("Integrity of the Send data:  "
                    + txResult + "\n");
            message = message.concat("Integrity of the Received data:  "
                    + rxResult + "\n");

            // Additionally, if there was an error in the CRC Calculation
            // notify
            // the user
            if (isValidRxData == false) {
               showResultDialogMessage("Data Integrity error detected during message reception");
            }
         } else {
            showResultDialogMessage("Please update your NTAG I2C Board firmware for the data integrity check");
         }

         // Transmission Results
         message = message.concat("Transfer NFC device to MCU\n");
         message = message.concat("Speed (" + bytes + " Byte / "
                 + readerTagTime + " ms): "
                 + String.format("%.0f", bytes / (readerTagTime / 1000.0))
                 + " Bytes/s\n");

         // Reception Results
         message = message.concat("Transfer MCU to NFC device\n");
         message = message.concat("Speed (" + bytes + " Byte / "
                 + tagReadTime + " ms): "
                 + String.format("%.0f", bytes / (tagReadTime / 1000.0))
                 + " Bytes/s");

         // Show data on the screen
         SpeedTestFragment.setAnswer("Test finished");
         SpeedTestFragment.setDatarateCallback(message);
      }
   }

   /**
    * Stops the SRAMSpeed Demo.
    */
   public void SRAMSpeedFinish() {
      if (sramspeedtask != null && !sramspeedtask.isCancelled()) {
         sramspeedtask.exit = true;
         try {
            sramspeedtask.get();
         } catch (Exception e) {
            e.printStackTrace();
            SharedCodes.Companion.log("NDemo", e.toString());

         }
         sramspeedtask = null;
      }
   }

   /**
    * Appends the CRC32 to the message transmitted during the SRAM SpeedTest.
    *
    * @param bytes The content to be transmitted to the NTAGI2C board
    * @return Byte Array with the CRC32s embedded in it
    */
   private byte[] appendCRC32(byte[] bytes) {
      byte[] temp = new byte[bytes.length - LAST_FOUR_BYTES];
      System.arraycopy(bytes, 0, temp, 0, temp.length);
      byte[] crc = CRC32Calculator.CRC32(temp);
      System.arraycopy(crc, 0, bytes, bytes.length - crc.length, crc.length);
      return bytes;
   }

   /**
    * Checks if the CRC32 value has been appended in the message.
    *
    * @param bytes The whole message received from the board
    * @return boolean that indicates the presence of the CRC32
    */
   private boolean isCRC32Appended(byte[] bytes) {
      for (int i = bytes.length - LAST_FOUR_BYTES; i < bytes.length; i++) {
         if (bytes[i] != 0x00)
            return true;
      }
      return false;
   }

   /**
    * Checks the received CRC32 value in the message received from the NTAG I2C
    * board during the SRAM SpeedTest.
    *
    * @param bytes The whole message received from the board
    * @return boolean with the result of the comparison between the CRC32
    * received and the CRC32 calculated
    */
   private boolean isValidCRC32(byte[] bytes) {
      byte[] receivedCRC = {bytes[bytes.length - LAST_FOUR_BYTES],
              bytes[bytes.length - 3],
              bytes[bytes.length - 2],
              bytes[bytes.length - 1]};

      byte[] temp = new byte[bytes.length - LAST_FOUR_BYTES];
      System.arraycopy(bytes, 0, temp, 0, bytes.length - LAST_FOUR_BYTES);
      byte[] calculatedCRC = CRC32Calculator.CRC32(temp);
      return Arrays.equals(receivedCRC, calculatedCRC);
   }

   /**
    * Performs the EEPROM Speedtest.
    *
    * @throws UnsupportedEncodingException
    * @throws IOException
    * @throws FormatException
    */
   public void EEPROMSpeedtest() throws UnsupportedEncodingException,
           IOException, FormatException {
      eepromspeedtask = new EEPROMSpeedtestTask(this);
      eepromspeedtask.execute();
   }

   // referenced in EEPROMSpeedTest
   private class EEPROMSpeedtestTask extends
           AsyncTask<Void, String, NdefMessage> {
      private WriteEEPROMListener listener;
      private Boolean exit = false;

      private long writeTime = 0;
      private long readTime = 0;

      private NdefMessage sendMessage;
      private int ndefMessageSize;

      public EEPROMSpeedtestTask(Ntag_I2C_Demo ntag_I2C_Demo) {
         this.listener = ntag_I2C_Demo;
      }

      @Override
      protected void onPreExecute() {
         try {
            SpeedTestFragment.setAnswer("EEPROM Speedtest");
            String textCharMulti = SpeedTestFragment
                    .getrf_ndef_value_charmulti();

            // getting text multiplier
            int chMultiplier = 1;
            int chMultiLength = textCharMulti.length();
            if (chMultiLength == 0) {
               chMultiplier = 1;
            } else {
               chMultiplier = Integer.parseInt(textCharMulti);
            }

            // building string
            String messageText = "";
            for (int i = 0; i < chMultiplier; i++) {
               messageText = messageText.concat(" ");
            }

            // check Memory Length
            Ntag_Get_Version.Prod prod = reader.getProduct();
            int max_Memsize = prod.getMemsize();

            // creating NDEF

            // NDEF is max Text length + 5 bytes
            sendMessage = createNdefMessage(messageText);
            ndefMessageSize = (sendMessage.toByteArray().length + 5);
            ndefMessageSize = (int) Math.round(ndefMessageSize / 4) * 4;

            // if NDEF is shorter as Memsize write_EEPROM else write an
            // error

            if (ndefMessageSize < max_Memsize) {
               SpeedTestFragment
                       .setDatarateCallback("writing in progress...");
               // Execute doInBackground
               return;
            } else {
               SpeedTestFragment.setAnswer("NDEF Message too Long");
               SpeedTestFragment
                       .setDatarateCallback("NDEF Message length: "
                               + ndefMessageSize
                               + "\n\rMaximum allowed: " + max_Memsize);
               cancel(true);
               return;
            }
         } catch (Exception e) {
            e.printStackTrace();
            SharedCodes.Companion.log("NDemo", e.toString());

            SpeedTestFragment.setDatarateCallback("Error try again");
            cancel(true);
            return;
         }
      }

      @Override
      protected NdefMessage doInBackground(Void... params) {
         try {
            // The NTAG I2C Plus might lose authentication when coming from writing/reading SRAM
            // so it is better to reauthenticate just in case
            Ntag_Get_Version.Prod prod = reader.getProduct();

            if (prod.equals(Ntag_Get_Version.Prod.NTAG_I2C_1k_Plus)
                    || prod.equals(Ntag_Get_Version.Prod.NTAG_I2C_2k_Plus)) {
               // Auth status gets lost after resetting the demo when we obtain the product we are dealing with
               if (PseudoMainActivity.getAuthStatus() == AuthStatus.Authenticated.getValue()) {
                  reader.authenticatePlus(PseudoMainActivity.getPassword());
               }
            }
            writeTime = System.currentTimeMillis();
            reader.writeNDEF(sendMessage, listener);
            writeTime = System.currentTimeMillis() - writeTime;

            if (exit) {
               cancel(true);
               return null;
            }
            publishProgress("writing finished \nreading in progress...");
            // Execute Read test
            readTime = System.currentTimeMillis();
            NdefMessage message = reader.readNDEF();
            readTime = System.currentTimeMillis() - readTime;

            if (exit) {
               cancel(true);
               return null;
            }
            publishProgress("writing finished \nreading finished");
            return message;
         } catch (CommandNotSupportedException e) {
            e.printStackTrace();
            SharedCodes.Companion.log("NDemo", e.toString());

            showDemoNotSupportedAlert();
            cancel(true);
            return null;
         } catch (Exception e) {
            e.printStackTrace();
            SharedCodes.Companion.log("NDemo", e.toString());

            publishProgress("Error while sending, try again");
            cancel(true);
            return null;
         }
      }

      @Override
      protected void onProgressUpdate(String... progress) {
         SpeedTestFragment.setDatarateCallback(progress[0]);
      }

      @Override
      protected void onPostExecute(NdefMessage message) {
         if (message == null) {
            toastText(main, "NDEF Read not supported",
                    Toast.LENGTH_LONG);
            return;
         }

         // Note: Write bytes value might be bigger than the Write bytes
         // value. This is because during the read process we read the whole
         // page

         // writing result to GUI
         String Message = "";
         Message = Message.concat("Transfer NFC device to MCU\n");
         Message = Message.concat("Speed (" + ndefMessageSize + " Byte / "
                 + writeTime + " ms): "
                 + String.format("%.0f", ndefMessageSize / (writeTime / 1000.0))
                 + " Bytes/s\n");

         // Reception Results
         Message = Message.concat("Transfer MCU to NFC device\n");
         Message = Message.concat("Speed (" + ndefMessageSize + " Byte / "
                 + readTime + " ms): "
                 + String.format("%.0f", ndefMessageSize / (readTime / 1000.0))
                 + " Bytes/s");

         SpeedTestFragment.setAnswer("Test finished");
         SpeedTestFragment.setDatarateCallback(Message);

         // Put an empty NDEF Message in the memory
         defaultNdeftask = new WriteDefaultNdefTask();
         defaultNdeftask.execute();
      }
   }

   /**
    * EEPROMSpeed finish
    */
   public void EEPROMSpeedFinish() {
      if (eepromspeedtask != null && !eepromspeedtask.isCancelled()) {
         eepromspeedtask.exit = true;
         try {
            eepromspeedtask.get();
         } catch (Exception e) {
            e.printStackTrace();
            SharedCodes.Companion.log("NDemo", e.toString());

         }
         eepromspeedtask = null;
      }
   }

   /**
    * Write empty ndef task.
    */
   // note: never used !!
   private class WriteEmptyNdefTask extends AsyncTask<Void, Void, Void> {

      @SuppressWarnings("unused")
      private Boolean exit = false;

      @Override
      protected Void doInBackground(Void... params) {
         try {
            reader.writeEmptyNdef();
         } catch (Exception e) {
            e.printStackTrace();
            SharedCodes.Companion.log("NDemo", e.toString());

            cancel(true);
            return null;
         }
         return null;
      }
   }

   /**
    * Write empty ndef finish.
    */
   public void WriteEmptyNdefFinish() {
      if (emptyNdeftask != null && !emptyNdeftask.isCancelled()) {
         emptyNdeftask.exit = true;
         try {
            emptyNdeftask.get();
         } catch (Exception e) {
            e.printStackTrace();
            SharedCodes.Companion.log("NDemo", e.toString());

         }
         emptyNdeftask = null;
      }
   }

   /**
    * Write default ndef task.
    */
   private class WriteDefaultNdefTask extends AsyncTask<Void, Void, Void> {

      @SuppressWarnings("unused")
      private Boolean exit = false;

      @Override
      protected Void doInBackground(Void... params) {
         try {
            reader.writeDefaultNdef();
         } catch (Exception e) {
            e.printStackTrace();
            SharedCodes.Companion.log("NDemo", e.toString());

            cancel(true);
            return null;
         }
         return null;
      }
   }

   /**
    * Write default ndef finish.
    */
   public void WriteDefaultNdefFinish() {
      if (defaultNdeftask != null && !defaultNdeftask.isCancelled()) {
         defaultNdeftask.exit = true;
         try {
            defaultNdeftask.get();
         } catch (Exception e) {
            e.printStackTrace();
            SharedCodes.Companion.log("NDemo", e.toString());

         }
         defaultNdeftask = null;
      }
   }

   @SuppressWarnings("unused")
   private NdefMessage creatNdefDefaultMessage()
           throws UnsupportedEncodingException {
      NdefRecord uri_record = NdefRecord
              .createUri("http://www.nxp.com/products/identification_and_security/"
                      + "smart_label_and_tag_ics/ntag/series/NT3H1101_NT3H1201.html");
      String text = "NTAG I2C Demoboard LPC812";
      String lang = "en";
      byte[] textBytes = text.getBytes();
      byte[] langBytes = lang.getBytes(StandardCharsets.US_ASCII);
      int langLength = langBytes.length;
      int textLength = textBytes.length;
      byte[] payload = new byte[1 + langLength + textLength];
      payload[0] = (byte) langLength;
      System.arraycopy(langBytes, 0, payload, 1, langLength);
      System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);
      NdefRecord textRecord = new NdefRecord(
              NdefRecord.TNF_WELL_KNOWN,
              NdefRecord.RTD_TEXT, new byte[0], payload);
      NdefRecord[] spRecords = {uri_record, textRecord};
      NdefMessage spMessage = new NdefMessage(spRecords);
      NdefRecord sp_record = new NdefRecord(
              NdefRecord.TNF_WELL_KNOWN,
              NdefRecord.RTD_SMART_POSTER, new byte[0],
              spMessage.toByteArray());
      NdefRecord aap_record = NdefRecord.createApplicationRecord(
              main.getPackageName());
      NdefRecord[] records = {sp_record, aap_record};
      NdefMessage message = new NdefMessage(records);
      return message;
   }

   /**
    * Creates a NDEF Text Message.
    *
    * @param text Text to write
    * @return NDEF Message
    * @throws UnsupportedEncodingException
    */
   private NdefMessage createNdefTextMessage(String text)
           throws UnsupportedEncodingException {
      if (text.length() == 0) {
         return null;
      }

      String lang = "en";
      byte[] textBytes = text.getBytes();
      byte[] langBytes = lang.getBytes(StandardCharsets.US_ASCII);
      int langLength = langBytes.length;
      int textLength = textBytes.length;
      byte[] payload = new byte[1 + langLength + textLength];
      payload[0] = (byte) langLength;
      System.arraycopy(langBytes, 0, payload, 1, langLength);
      System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);
      NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
              NdefRecord.RTD_TEXT, new byte[0], payload);
      NdefRecord[] records = {record};

      SharedCodes.Companion.log("createNdefTextMessage", "text:" + text);
      SharedCodes.Companion.log("createNdefTextMessage", "text bytes:" + FlutterNtag_I2C_Demo.Companion.memStringToPrintableByte(text));
      SharedCodes.Companion.log("createNdefTextMessage", "langLength:" + langLength + ", textLength: " + textLength);
      SharedCodes.Companion.log("createNdefTextMessage", "payload l:" + (1 + langLength + textLength));

      return new NdefMessage(records);
   }

   /**
    * Creates a NDEF Text Message.
    *
    * @param text Text to write
    * @return NDEF Message
    * @throws UnsupportedEncodingException
    */
   private NdefMessage createNdefBytesMessage(byte[] bytes)
           throws UnsupportedEncodingException {
      if (bytes.length == 0) {
         return null;
      }
      String lang = "en";
      byte[] textBytes = bytes;
      byte[] langBytes = lang.getBytes(StandardCharsets.US_ASCII);
      int langLength = langBytes.length;
      int textLength = textBytes.length;
      byte[] payload = new byte[1 + langLength + textLength];
      payload[0] = (byte) langLength;
      System.arraycopy(langBytes, 0, payload, 1, langLength);
      if (SharedCodes.Companion.getCustomWrite()) {
         byte[] inserted = new byte[SharedCodes.Companion.getShiftBits() + textBytes.length];
         System.arraycopy(textBytes, 0, inserted, 3, textBytes.length);
         textBytes = inserted;
      }
      System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);
      NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);
      NdefRecord[] records = {record};

      SharedCodes.Companion.log("createNdefTextMessage", "bytes:" + bytes.toString());
      SharedCodes.Companion.log("createNdefTextMessage", "langLength:" + langLength + ", textLength: " + textLength);
      SharedCodes.Companion.log("createNdefTextMessage", "payload l:" + (1 + langLength + textLength));

      return new NdefMessage(records);
   }

   /**
    * Creates a NDEF Uri Message.
    *
    * @param uri Uri to write
    * @return NDEF Message
    */
   private NdefMessage createNdefUriMessage(String uri) {
      if (uri.length() == 0
              || uri.endsWith("//")
              || uri.endsWith(":")
              || uri.endsWith(".")) {
         return null;
      }
      NdefRecord record = NdefRecord.createUri(uri);
      NdefRecord[] records = {record};
      return new NdefMessage(records);
   }

   /**
    * Creates a Bluetooth Secure Simple Pairing Message.
    * <p>
    * <p>
    * Bluetooth MAC Address to Write
    *
    * @return NDEF Message
    */
   private NdefMessage createNdefBSSPMessage() {
      byte[] payloadHs = {0x12, (byte) 0xD1, 0x02, 0x04, 0x61, 0x63, 0x01, 0x01, 0x30, 0x00};
      NdefRecord recordHs = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
              NdefRecord.RTD_HANDOVER_SELECT, new byte[0], payloadHs);

      byte[] deviceMAC = hexStringToByteArray(NdefFragment.getBtMac().replaceAll(":", ""));
      byte[] deviceName = NdefFragment.getBtName().getBytes();
      byte[] deviceClass = hexStringToByteArray(NdefFragment.getBtClass().replaceAll(":", ""));

      if (deviceMAC.length != 6
              || deviceName.length == 0
              || deviceClass.length != 3) {
         return null;
      }
      byte[] payloadBt = new byte[deviceMAC.length + deviceName.length
              + deviceClass.length + 2 + 4];

      // Payload Size
      payloadBt[0] = (byte) payloadBt.length;

      System.arraycopy(deviceMAC, 0, payloadBt, 2, deviceMAC.length);
      payloadBt[8] = (byte) (deviceName.length + 1);
      payloadBt[9] = 0x09; // Device Name identifier
      System.arraycopy(deviceName, 0, payloadBt, 10, deviceName.length);
      payloadBt[8 + deviceName.length + 2] = (byte) (deviceClass.length + 1);
      payloadBt[8 + deviceName.length + 3] = 0x0D; // Service Name identifier
      System.arraycopy(deviceClass, 0, payloadBt, 8 + deviceName.length + 4,
              deviceClass.length);
      NdefRecord recordBt = new NdefRecord(NdefRecord.TNF_MIME_MEDIA,
              "application/vnd.bluetooth.ep.oob".getBytes(), new byte[0],
              payloadBt);
      NdefRecord[] records = {recordHs, recordBt};
      return new NdefMessage(records);
   }

   /**
    * Creates a NDEF SmartPoster Message.
    *
    * @param uri   Uri to write
    * @param title Text to write
    * @return NDEF Message
    * @throws UnsupportedEncodingException
    */
   private NdefMessage createNdefSpMessage(String title, String uri) throws UnsupportedEncodingException {
      if (title.length() == 0)
         return null;

      if (uri.length() == 0
              || uri.endsWith("//")
              || uri.endsWith(":")
              || uri.endsWith(".")) {
         return null;
      }

      String lang = "en";
      byte[] textBytes = title.getBytes();
      byte[] langBytes = lang.getBytes(StandardCharsets.US_ASCII);
      int langLength = langBytes.length;
      int textLength = textBytes.length;
      byte[] payload = new byte[1 + langLength + textLength];
      payload[0] = (byte) langLength;
      System.arraycopy(langBytes, 0, payload, 1, langLength);
      System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

      NdefRecord recordTitle = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
              NdefRecord.RTD_TEXT, new byte[0], payload);
      NdefRecord recordLink = NdefRecord.createUri(uri);

      NdefRecord[] records = new NdefRecord[]{recordTitle, recordLink};
      NdefMessage messageSp = new NdefMessage(records);

      byte[] bytes = messageSp.toByteArray();
      NdefRecord recordSp = new NdefRecord((short) 0x01,
              NdefRecord.RTD_SMART_POSTER, null, bytes);

      return new NdefMessage(new NdefRecord[]{recordSp});
   }

   public static byte[] hexStringToByteArray(String s) {
      int len = s.length();
      byte[] data = new byte[len / 2];
      for (int i = 0, j = (len - 2) / 2; i < len; i += 2, j--) {
         data[j] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character
                 .digit(s.charAt(i + 1), 16));
      }
      return data;
   }

   /**
    * Closes the Reader, Writes the NDEF Message via Android function and
    * reconnects the Reader.
    *
    * @return NDEF Writing time
    * @throws IOException
    * @throws FormatException
    * @throws CommandNotSupportedException
    **/
   private long NDEFWrite(NdefMessage msg) throws IOException, FormatException, CommandNotSupportedException {
      // Time statistics to return
      long timeNdefWrite = 0;
      long RegTimeOutStart = System.currentTimeMillis();
      // Write the NDEF using NfcA commands to avoid problems when dealing with protected tags
      // Calling reader close / connect resets the authenticated status
      SharedCodes.Companion.log("NDEFWrite", "...");
      reader.writeNDEF(msg, null);

      timeNdefWrite = System.currentTimeMillis() - RegTimeOutStart;
      // Return the calculated time value
      return timeNdefWrite;
   }

   /**
    * Closes the Reader, Writes the NDEF Message via Android function and
    * reconnects the Reader.
    *
    * @return NDEF Writing time
    * @throws IOException
    * @throws FormatException
    * @throws CommandNotSupportedException
    **/
   private long NDEFWriteCustom(NdefMessage msg) throws IOException, FormatException, CommandNotSupportedException {
      // Time statistics to return
      long timeNdefWrite = 0;
      long RegTimeOutStart = System.currentTimeMillis();
      // Write the NDEF using NfcA commands to avoid problems when dealing with protected tags
      // Calling reader close / connect resets the authenticated status
      SharedCodes.Companion.log("NDEFWriteCustom", "...");
      reader.writeNDEFCustom(msg, null);

      timeNdefWrite = System.currentTimeMillis() - RegTimeOutStart;
      // Return the calculated time value
      return timeNdefWrite;
   }

   /**
    * Calculates the Temperature in Celsius.
    *
    * @param temp Temperature
    * @return String of Temperature in Dez
    */
   private String calcTempCelsius(int temp) {
      double tempDouble = 0;
      String tempString = "";
      // If the 11 Bit is 1 it is negative
      if ((temp & (1 << 11)) == (1 << 11)) {
         // Mask out the 11 Bit
         temp &= ~(1 << 11);
         tempString += "-";
      }
      tempDouble = 0.125 * temp;
      // Update the value on the Led fragment
      LedFragment.setTemperatureC(tempDouble);
      DecimalFormat df = new DecimalFormat("#.00");
      tempString = df.format(tempDouble);
      return tempString;
   }

   /**
    * Calculates the Temperature in Farenheit.
    *
    * @param temp Temperature
    * @return String of Temperature in Dez
    */
   private String calcTempFarenheit(int temp) {
      double tempDouble = 0;
      String tempString = "";

      // If the 11 Bit is 1 it is negative
      if ((temp & (1 << 11)) == (1 << 11)) {
         // Mask out the 11 Bit
         temp &= ~(1 << 11);
         tempString += "-";
      }
      tempDouble = 0.125 * temp;
      tempDouble = 32 + (1.8 * tempDouble);
      LedFragment.setTemperatureF(tempDouble);
      DecimalFormat df = new DecimalFormat("#.00");
      tempString = df.format(tempDouble);
      return tempString;
   }

   /**
    * Calculates the Voltage.
    *
    * @param volt Voltage
    * @return String Voltage value
    */
   private String calcVoltage(int volt) {
      String voltString = "0.0";

      if (volt > 0) {
         double Volt_double = round((0x3FF * 2.048) / volt);

         // Update the value on the Led fragment
         LedFragment.setVoltage(Volt_double);
         DecimalFormat df = new DecimalFormat("0.0");
         voltString = df.format(Volt_double) + "V";
      } else {
         voltString = "Not available";
      }
      return voltString;
   }

   /**
    * Rounds the voltage to one single decimal.
    */
   public double round(double value) {
      return Math.rint(value * 10) / 10;
   }

   /**
    * Helper function to show messages on the screen.
    *
    * @param temp Message
    */
   protected void showResultDialogMessage(String temp) {
      //fixme:

      new AlertDialog.Builder(main).setMessage(temp)
              .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                 @Override
                 public void onClick(DialogInterface dialog, int which) {
                 }
              }).show();
   }

   /**
    * Creates a NDEF Message
    *
    * @param text Text to write
    * @return NDEF Message
    * @throws UnsupportedEncodingException description
    */
   private NdefMessage createNdefMessage(String text)
           throws UnsupportedEncodingException {
      String lang = "en";
      byte[] textBytes = text.getBytes();
      byte[] langBytes = lang.getBytes(StandardCharsets.US_ASCII);
      int langLength = langBytes.length;
      int textLength = textBytes.length;
      byte[] payload = new byte[1 + langLength + textLength];
      payload[0] = (byte) langLength;
      System.arraycopy(langBytes, 0, payload, 1, langLength);
      System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

      NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
              NdefRecord.RTD_TEXT, new byte[0], payload);
      NdefRecord[] records = {record};
      return new NdefMessage(records);
   }

   protected byte[] concat(byte[] one, byte[] two) {
      if (one == null) {
         one = new byte[0];
      }
      if (two == null) {
         two = new byte[0];
      }
      byte[] combined = new byte[one.length + two.length];
      System.arraycopy(one, 0, combined, 0, one.length);
      System.arraycopy(two, 0, combined, one.length, two.length);
      return combined;
   }

   @Override
   public void onWriteEEPROM(final int bytes) {
      final String datarate = String.valueOf(bytes) + " Bytes written";
      main.runOnUiThread(new Runnable() {
         @Override
         public void run() {
            SpeedTestFragment.setDatarateCallback(String.valueOf(bytes) + " Bytes written");
         }
      });
      if (onWriteEEPROMHandler != null) {
         onWriteEEPROMHandler.onWriteEEPROM(bytes);
      }
   }

   @Override
   public void onWriteSRAM() {
      main.runOnUiThread(new Runnable() {
         @Override
         public void run() {
            FlashMemoryActivity.updateFLashDialog();
         }
      });
      if (onWriteSRAMHandler != null) {
         onWriteSRAMHandler.onWriteSRAM();
      }
   }
}
