package com.example.javaclient.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.widget.Toast;

import com.example.javaclient.activities.MainScreenActivity;

import java.io.IOException;
import java.util.ArrayList;

public class ClientHandler extends AsyncTask<String, Void, ResponseFormat> {

    private String address;
    private Client client;
    private Context context;
    private Flags currentFlag;
    private User currentUser;

    private Thread messageThread;
    private Thread sendingThread;

    public ClientHandler(Context context, String address) {
        this.address = address;
        this.context = context;
        currentUser = null;
        client = null;
    }

    public void stopListening() {
        if(messageThread != null)
            messageThread.stop();
    }

    public void startListening(final Context activity) {
        messageThread = new Thread(new Runnable() {

            String response;
            Handler handler = new Handler();
            @Override
            public void run() {
                synchronized (this) {
                    try {
                        response = client.getInputStream().readUTF();
                        if(response.length() > 0)
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                AlertDialog dialog = new AlertDialog.Builder(activity)
                                        .setTitle("Message")
                                        .setMessage(response)
                                        .setPositiveButton("OK", null).show();
                                Toast.makeText(context, response, Toast.LENGTH_SHORT).show();
                            }
                        }, 500);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        messageThread.start();
    }

    public void sendMessage(final String message) {
        sendingThread = new Thread(new Runnable() {

            MessagePacket messagePacket;
            ResponseFormat responseFormat;
            RequestFormat requestFormat;
            @Override
            public void run() {
                synchronized (this) {
                    messagePacket = new MessagePacket(currentUser.getUsername(), message, 0);
                    requestFormat = new RequestFormat(Flags.MESSAGE, client.getG().toJson(messagePacket));
                    responseFormat = client.ExecCommand(Flags.MESSAGE, client.getG().toJson(requestFormat));
                    System.out.println(client.getG().toJson(messagePacket) + "");
                    if(responseFormat != null) {
                        if(responseFormat.status.equals(com.example.javaclient.utils.Status.OK)) {
                            Toast.makeText(context, "Message has been broadcasted!", Toast.LENGTH_SHORT);
                        }

                        sendingThread.stop();
                    }
                }
             }
        });

        sendingThread.start();
    }

    @Override
    protected ResponseFormat doInBackground(String... voids) {
        Flags flag = Flags.valueOf(voids[0].toUpperCase());
        String secondArg = voids[1];
        String username = secondArg.split(" ")[0];
        String password = secondArg.split(" ")[1];
        currentFlag = flag;
        client = new Client(address);
        currentUser = new User(username, password);
        ResponseFormat response = client.ExecCommand(flag, secondArg);
        return response;
    }

    @Override
    protected void onPostExecute(ResponseFormat response) {
        com.example.javaclient.utils.Status status = response.status;
        if(status.equals(com.example.javaclient.utils.Status.OK)) {
            Toast.makeText(context, currentFlag.getMessage(), Toast.LENGTH_SHORT).show();
            makeAction(currentFlag);
        } else if(status.equals(com.example.javaclient.utils.Status.NOTFOUND)) {
            Toast.makeText(context, "User not found!", Toast.LENGTH_SHORT).show();
        } else if(status.equals(com.example.javaclient.utils.Status.BAD_REQUEST)) {
            Toast.makeText(context, "User already exists!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "There seems to be an error connection!", Toast.LENGTH_SHORT).show();
        }
    }

    private void makeAction(Flags flag) {
        switch(flag) {
            case LOGIN: {
                //Save data to shared preferences
                SharedPreferences storage = context.getSharedPreferences("Storage", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = storage.edit();
                editor.putString("username", currentUser.getUsername());
                editor.putString("password", currentUser.getPassword());
                editor.commit();

                //Move to the main activity and show all data needed for client
                Intent intent = new Intent(context, MainScreenActivity.class);
                intent.putExtra("user", currentUser);
                context.startActivity(intent);
            } break;
            case REGISTER: {
                ((Activity) context).finish();
            } break;
        }
    }
}
