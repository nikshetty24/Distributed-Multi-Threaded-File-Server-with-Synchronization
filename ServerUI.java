/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package my.ServerUI;

import java.net.*;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;

class testThread implements Runnable {

    String type;

    ServerSocket welcomeSocket;
    DataOutputStream dout;
    DataInputStream din;
    DefaultListModel dlm;
    ServerUI ui;
    File f;

    testThread(ServerSocket ws, DefaultListModel dm, ServerUI sui) {
        welcomeSocket = ws;
        dlm = dm;
        ui = sui;
    }

    class ServeOneJabber extends Thread {

        private Socket socket;

        public ServeOneJabber(Socket s)
                throws IOException {
            socket = s;
            din = new DataInputStream(s.getInputStream());

            dout = new DataOutputStream(s.getOutputStream());
            // If any of the above calls throw an 
            // exception, the caller is responsible for
            // closing the socket. Otherwise the thread
            // will close it.
            start(); // Calls run()
        }
    }

    @Override
    public void run() {
        boolean isClientConnected = false;
        while (true) {
            try {
                if (!isClientConnected) {

                    Socket s = welcomeSocket.accept();
                    try {
                        new ServeOneJabber(s);
                    } catch (IOException e) {
                        // If it fails, close the socket,
                        // otherwise the thread will close it:
                        s.close();
                    }
                    dlm.addElement("Client Connected ");

                    isClientConnected = true;
                }
                //String check="true";
                //String test=din.readUTF();
                // while(!check.equals(test))
                //{
                
                type = din.readUTF();
                switch (type) {
                    case "down":
                        System.out.println("down");
                        String file = din.readUTF();
                        System.out.println(file);
                        download(file);
                        break;
                    case "delete":
                        System.out.println("delete");
                        file = din.readUTF();
                        delete(file);
                        break;
                    case "rename":
                        System.out.println("rename");
                        file = din.readUTF();
                        String nfile = din.readUTF();
                        rename(file, nfile);
                        break;
                    case "upload":
                        System.out.println("Upload");
                        upload();
                        break;
                    case "display":
                        display();
                        break;
                    
                }

            } catch (Exception e) {
                isClientConnected = false;
                dlm.addElement(e);
                e.printStackTrace();
            }
            ui.SetList(dlm);
        }

    }

    public void upload()  {
        try { 
            
            ///Check is pending Already Uploaded file
            String str = din.readUTF();
             File file = new File("C:\\Server\\" + str);
             if(!file.exists()){
             dout.writeUTF("1");
            dlm.addElement("Receving file: " + str);
           
            dlm.addElement("Saving as file: " + str);
            //
            long sz = Long.parseLong(din.readUTF());
            dlm.addElement("File Size: " + (sz / (1024 * 1024)) + " MB");

            byte b[] = new byte[1024];
            dlm.addElement("Receving file..");
            FileOutputStream fos = new FileOutputStream(new File(str), true);
            long bytesRead;
            do {
                bytesRead = din.read(b, 0, b.length);
                fos.write(b, 0, b.length);
            } while (!(bytesRead < 1024));
            dlm.addElement("Completed");
            fos.close();
             }else{
                 dout.writeUTF("0");
             }
            
        } catch (Exception e) {
            //do nothing
        }
        ui.SetList(dlm);
    }
    public void display(){
        File folder = new File("C:\\Server\\");
        File[] listOfFiles = folder.listFiles();
         
    try{
        dout.writeInt(listOfFiles.length);
    for (int i = 0; i < listOfFiles.length; i++) {
      if (listOfFiles[i].isFile()) {
          dout.writeUTF(listOfFiles[i].getName());
        dlm.addElement("File " + listOfFiles[i].getName());
      } 
    }
    
    }catch(Exception e){
        
    }
    ui.SetList(dlm);
    }

    public void rename(String filename, String newname) {
        System.out.println(filename);
        System.out.println(newname);
      
        File oldfile = new File("C:\\Server\\" + filename);
        File newfile = new File("C:\\Server\\" + newname);
        if (oldfile.renameTo(newfile)) {
            dlm.addElement("File name changed succesful");
            try {
                dout.writeUTF("1");
                dout.flush();
            } catch (IOException ex) {
                Logger.getLogger(testThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            dlm.addElement("Rename failed");
            try {
                dout.writeUTF("0");
                dout.flush();
            } catch (IOException ex) {
                Logger.getLogger(testThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        ui.SetList(dlm);

    }

    public void delete(String filename) {
        File file = new File("C:\\Server\\" + filename);
        if (file.delete()) {

            try {
                dout.writeUTF("1");
                dout.flush();
            } catch (IOException ex) {
                Logger.getLogger(testThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            try {
                dout.writeUTF("0");
                dout.flush();
            } catch (IOException ex) {
                Logger.getLogger(testThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    public void download(String filename) {
        
        f = new File("C:\\Server\\" + filename);
        if (f.exists()) {
            try {
                dout.flush();
                dout.writeUTF("1");
                dout.flush();

                FileInputStream fin = new FileInputStream(f);
                long sz = (int) f.length();

                byte b[] = new byte[1024];

                int read;

                dout.writeUTF(Long.toString(sz));
                dout.flush();

                dlm.addElement("Size: " + sz);
                dlm.addElement("Buf size: " + welcomeSocket.getReceiveBufferSize());

                while ((read = fin.read(b)) != -1) {
                    dout.write(b, 0, read);
                    dout.flush();
                }
                fin.close();

                dlm.addElement("..ok");
                dout.flush();

                dout.writeUTF("stop");
                dlm.addElement("Send Complete");
                dout.flush();
            } catch (Exception e) {
                e.printStackTrace();
                dlm.addElement("An error occured");
            }
        } else {

            try {
                dout.writeUTF("0");
            } catch (IOException ex) {
                Logger.getLogger(testThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        ui.SetList(dlm);

    }

}

public class ServerUI extends javax.swing.JFrame {

    boolean isServerStarted;
    ServerSocket welcomeSocket;
    DefaultListModel dlm = new DefaultListModel();
    Thread thread;

    public ServerUI() {
        initComponents();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        DisplayArea = new javax.swing.JList();
        StartServer = new javax.swing.JButton();
        Quit = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Messages"));

        jScrollPane1.setViewportView(DisplayArea);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 368, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 323, Short.MAX_VALUE)
        );

        StartServer.setText("Start Server");
        StartServer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StartServerActionPerformed(evt);
            }
        });

        Quit.setText("Quit");
        Quit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                QuitActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(StartServer)
                .addGap(32, 32, 32)
                .addComponent(Quit)
                .addGap(33, 33, 33))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(StartServer)
                    .addComponent(Quit))
                .addGap(52, 52, 52))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void StartServerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StartServerActionPerformed
        try {
            welcomeSocket = new ServerSocket(6982);
            dlm.addElement("server started..");
            dlm.addElement("Server Waiting for Connections on Port 6789");
            DisplayArea.setModel(dlm);
            displayfull();
        } catch (Exception e) {
            dlm.addElement(e);
        }
        testThread tt = new testThread(welcomeSocket, dlm, this);
        thread = new Thread(tt);
        thread.start();
        DisplayArea.setModel(dlm);
        displayfull();
    }//GEN-LAST:event_StartServerActionPerformed

    private void QuitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_QuitActionPerformed
        // TODO add your handling code here:
        System.exit(0);
    }//GEN-LAST:event_QuitActionPerformed

    public void SetList(DefaultListModel dLM) {
        DisplayArea.setModel(dLM);
        DisplayArea.ensureIndexIsVisible(dlm.getSize() - 1);
    }

    void displayfull() {
        DisplayArea.ensureIndexIsVisible(dlm.getSize() - 1);
    }

    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(ServerUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ServerUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ServerUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ServerUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ServerUI().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JList DisplayArea;
    private javax.swing.JButton Quit;
    private javax.swing.JButton StartServer;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables
}
