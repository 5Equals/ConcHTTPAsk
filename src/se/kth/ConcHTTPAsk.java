/**
 * Software that enables the person to make multiple requests at the same time and receive answers from the sources
 * simultaneously.
 */

package se.kth;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class ConcHTTPAsk {
    public static void main(String[] args) {
        try {
            ServerSocket myServer = startServer(123);
            while (true) {
                Socket clientConnection = HTTPAsk.listenToClient(myServer);
                ServeClient client = new ServeClient(clientConnection);
                Thread clientThread = new Thread(client);
                clientThread.start();
            }
        } catch (IOException exception) {
            exception.printStackTrace();
            System.exit(1);
        }
    }

    static ServerSocket startServer(int port) throws IOException {
        return new ServerSocket(port);
    }
}

class TCPClient {
    static String askServer(String hostname, int port, String ToServer) throws IOException {
        Socket clientConnection = establishConnection(hostname, port);
        sendMessageToServer(clientConnection, ToServer);
        String responseResult = getServerResponse(clientConnection);
        closeConnection(clientConnection);
        return responseResult;
    }

    static String askServer(String hostname, int port) throws IOException{
        Socket clientConnection = establishConnection(hostname, port);
        String responseResult = getServerResponse(clientConnection);
        closeConnection(clientConnection);
        return responseResult;
    }

    static Socket establishConnection(String hostname, int port) throws IOException {
        Socket connection = new Socket();
        // Socket time out exception
        connection.connect(new InetSocketAddress(hostname, port) , 5000);
        return connection;
    }

    static void closeConnection(Socket connection) throws IOException {
        connection.close();
    }

    static void sendMessageToServer(Socket connection, String message) throws IOException {
        OutputStream sendQuery = connection.getOutputStream();
        sendQuery.write(encodeMessage(message));
    }

    static String getServerResponse(Socket connection) throws IOException {
        InputStream getResponse = connection.getInputStream();
        byte[] response = new byte[2048];
        int i = 0;
        connection.setSoTimeout(2000);
        try {
            while(true) {
                int data = getResponse.read();
                if(i == response.length) {
                    response = doubleEnlargeArray(response);
                }
                if (data == -1) {
                    break;
                }
                response[i++] = (byte) data;
            }
        } catch (Exception exception) {
            exception.getMessage();
        }
        byte[] filteredResponse = resizeArray(response, i);
        return decodeMessage(filteredResponse);
    }

    static byte[] doubleEnlargeArray(byte[] array) {
        byte[] doubleSizeArray = new byte[array.length * 2];
        for (int i = 0; i < array.length; i++) {
            doubleSizeArray[i] = array[i];
        }
        return doubleSizeArray;
    }

    static byte[] resizeArray(byte[] array, int correctSize) {
        byte[] newArray = new byte[correctSize];
        for(int i = 0; i < newArray.length; i++) {
            newArray[i] = array[i];
        }
        return newArray;
    }

    static byte[] encodeMessage(String message) {
        return message.getBytes(StandardCharsets.UTF_8);
    }

    static String decodeMessage(byte[] message) {
        return new String(message, StandardCharsets.UTF_8);
    }
}

class HTTPAsk {
    static String[] getRequestResults (String request) throws IOException {
        String[] results = new String[2];
        try {
            String filterRequest =  filterHTTPRequest(request);
            String[] requestParameters = getRequestParameters(filterRequest);
            if (requestParameters.length == 2) {
                results[0] = TCPClient.askServer(requestParameters[0], Integer.parseInt(requestParameters[1]));
            } else if (requestParameters.length == 3) {
                results[0] = TCPClient.askServer(requestParameters[0], Integer.parseInt(requestParameters[1]), requestParameters[2] + "\n");
            }
        } catch (UnknownHostException | SocketTimeoutException exception) {
            results[0] = "\nThe page is not found or the host could not be reached!";
        } catch (IllegalCallerException exception) {
            results[0] = "\nCould not find the file. Try to use /ask instead." ;
            results[1] = "HTTP/1.1 404 Not Found";
            return results;
        }
        catch (Exception exception) {
            results[0] = "Server usage: \nhttp://hostname:port/ask?hostname=<host>&port=<port number>\noptional: " +
                    "http://hostname:port/ask?hostname=<host>&port=<port number>&string=<text to send>";
            results[1] = "HTTP/1.1 400 Bad Request";
            return results;
        }
        results[1] = "HTTP/1.1 200 OK";
        return results;
    }

    static String filterHTTPRequest(String request) throws IllegalArgumentException {
        String splitString = request.split("\r\n")[0];
        if(!splitString.matches("GET .* HTTP/1\\.1")) {
            throw new IllegalArgumentException();
        }
        return splitString.split(" ")[1];
    }

    static String[] getRequestParameters(String filteredRequest) {
        if(!filteredRequest.contains("/ask")) {
            throw new IllegalCallerException();
        }
        if(!filteredRequest.matches("/ask\\?hostname=.*&port=.*")) {
            throw new IllegalArgumentException();
        }

        return fetchRequestParameters(filteredRequest);
    }

    static String[] fetchRequestParameters(String request) {
        String parameters = "";
        char[] tempContent = new char[request.indexOf("&port=") - request.indexOf("hostname=") - 9];
        request.getChars(request.indexOf("hostname=") + 9, request.indexOf("&port="), tempContent,0);
        parameters += new String(tempContent) + " ";

        if(request.contains("&string=")) {
            tempContent = new char[request.indexOf("&string=") - request.indexOf("&port=") - 6];
            request.getChars(request.indexOf("&port=") + 6, request.indexOf("&string="), tempContent,0);
            parameters += new String(tempContent) + " ";

            tempContent = new char[request.length() - request.indexOf("&string=") - 8];
            request.getChars(request.indexOf("&string=") + 8, request.length(), tempContent,0);
            parameters += new String(tempContent);
        } else {
            tempContent = new char[request.length() - request.indexOf("&port=") - 6];
            request.getChars(request.indexOf("&port=") + 6, request.length(), tempContent, 0);
            parameters += new String(tempContent);
        }

        String[] resultParameters = parameters.split(" ");
        if(resultParameters.length == 3) {
            resultParameters[2] = resultParameters[2].replaceAll("%20", " ");
        }
        return resultParameters;
    }

    static Socket listenToClient(ServerSocket server) throws IOException {
        return server.accept();
    }

    static void stopListeningToClient(Socket connection) throws IOException {
        connection.close();
    }

    static void sendDataToClient(Socket connection, String data, String responseStatus) throws IOException {
        OutputStream sendResponse = connection.getOutputStream();
        sendResponse.write(TCPClient.encodeMessage(responseStatus + "\r\n\r\n"));
        sendResponse.write(TCPClient.encodeMessage(data + "\r\n"));
    }


    static String getDataFromClient(Socket connection) throws IOException {
        InputStream getResponse = connection.getInputStream();
        byte[] response = new byte[2048];
        int i = 0;
        connection.setSoTimeout(2000);
        try {
            while(true) {
                if(i > 4) {
                    if(response[i - 1] == 10 &&
                            response[i - 2] == 13 &&
                            response[i - 3] == 10 &&
                            response[i - 4] == 13) {
                        break;
                    }
                }
                int data = getResponse.read();
                if(i == response.length) {
                    response = TCPClient.doubleEnlargeArray(response);
                }
                if (data == -1) {
                    break;
                }
                response[i++] = (byte) data;
            }
        } catch (Exception exception) {
            exception.getMessage();
        }
        byte[] filteredResponse = TCPClient.resizeArray(response, i);
        return TCPClient.decodeMessage(filteredResponse);
    }
}

class ServeClient implements Runnable {

    Socket clientConnection = null;

    ServeClient(Socket clientConnection) {
        this.clientConnection = clientConnection;
    }

    @Override
    public void run() {
        try {
            System.out.println(this.clientConnection.toString());
            String HTTPRequest = HTTPAsk.getDataFromClient(this.clientConnection);
            String[] requestResult = HTTPAsk.getRequestResults(HTTPRequest);
            HTTPAsk.sendDataToClient(this.clientConnection, requestResult[0], requestResult[1]);
            HTTPAsk.stopListeningToClient(this.clientConnection);
        } catch (IOException exception) {
            exception.printStackTrace();
            System.exit(1);
        }
    }
}