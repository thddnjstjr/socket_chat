package project1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Vector;

import lombok.Data;
import lombok.Locked.Write;

@Data
public class Server2 {
	
	private static Vector<Socket>socket;
	private static ArrayList<String>roomName;
	private static ArrayList<String>userId;
	private static ArrayList<String>createUser;
	private static Kakao kakao;
	private static PrintWriter writer;
	private static BufferedReader client;
	private static int number;
	private static int socketNum;
	private String name;
	private String serverName;
	private ServerFrame serverFrame;
	private static int port = 5001;
	
	public Server2() {
		
		socket = new Vector<>(30); // 인원 30명으로 설정
		createUser = new ArrayList<>(5);
		roomName = new ArrayList<>(20);
		userId = new ArrayList<>(30);
		
		try (ServerSocket serverSocket = new ServerSocket(5000)){
			System.out.println("서버 구동완료");
			while(true) {
				Socket blank = serverSocket.accept();
				socket.add(blank);
				new Service(socket.get(socketNum)).start();
				socketNum++;
				System.out.println(socketNum);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	private static class MakeServer implements Runnable {
		public static Vector<Socket>chatSocket;
		private static ArrayList<Integer> user;
		private static int port;
		private static int serialNum;
		
		public MakeServer(int port) {
			this.port = port;
		}
		
		@Override
		public void run() {
			chatSocket = new Vector<>(30);
			try(ServerSocket chatServer = new ServerSocket(port)) {
				System.out.println("서버 생성 완료");
				System.out.println(port);
				while(true) {
					System.out.println(port + "서버 기다리는중");
					chatSocket.add(chatServer.accept());
					System.out.println("손님 입장");
					System.out.println(chatSocket.size());
					System.out.println(serialNum);
					new Client(chatSocket.get(serialNum), serialNum).start();
					serialNum++;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		public static Vector<Socket> getChatSocket() {
			return chatSocket;
		}
		
	}
	
	// 전체에게 명령이 들어가는 메소드 (생성 명령)
	private static void broadCast(String message,int port) {
		for(Socket socket : socket) {
			try {
				writer = new PrintWriter(socket.getOutputStream(), true);
				writer.println("createroom:"+message);
				writer.println("portNum:"+port);
				System.out.println(message + "방이름 전송");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	// 전체에게 명령이 들어가는 메소드 (삭제 명령)
	private static void broadCast(int indexNum) {
		for(Socket socket : socket) {
			try {
				writer = new PrintWriter(socket.getOutputStream(), true);
				writer.println("deleteroom:"+indexNum);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	// 전체에게 메세지가 들어가는 메소드 
	private static void broadCastChat(String message) {
		for(Socket socket : MakeServer.getChatSocket()) {
			try {
				writer = new PrintWriter(socket.getOutputStream(), true);
				writer.println(message);
				System.out.println(message);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	// 채팅 서버 스레드
	private static class Client extends Thread{
		
		private Socket socket;
		private BufferedReader msgin;
		private int serialNumber;
		
		public Client(Socket socket, int number) {
			this.socket = socket;
			this.serialNumber = number;
		}
		
		@Override
		public void run() {
			try {
				System.out.println("스레드 작동");
				msgin = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				writer = new PrintWriter(socket.getOutputStream());
				String message;
				while( (message = msgin.readLine()) != null) {
					if(message.startsWith("quit")){
						String exit[] = message.split(":");
						broadCastChat(exit[1]);
					} else {
						broadCastChat(message);
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		
	}

	// 서비스 서버 스레드
	private static class Service extends Thread {
		
		private Socket socket;
		private BufferedReader order;
		private PrintWriter service;
		
		public Service(Socket socket) {
			this.socket = socket;
			System.out.println("서비스 시작");
		}
		
		@Override
		public void run() {
			try {
				order = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				service = new PrintWriter(socket.getOutputStream(), true);
				String orderMsg;
				for(int i = 0; i < roomName.size(); i++) { // 처음 들어온 사용자에게 있는 방 리스트를 전송
					service.println("createroom:" + roomName.get(i));
					System.out.println(roomName.get(i));
				}
				while( (orderMsg = order.readLine()) != null) {
					System.out.println(orderMsg + "확인");
					if(orderMsg.startsWith("createroom")) { // 방 생성 하라는 명령어
						String[] serverName = orderMsg.split(":");
						broadCast(serverName[1],port); // 대기실 서버 사용자들에게 해당 이름의 방을 만들라고 명령 , 포트 주소도 보냄
						roomName.add(serverName[1]); // 서버에 방 추가
						createUser.add(serverName[2]); // 만든유저 정보 저장
						new Thread(new MakeServer(port)).start();
						port++;
					}
					if(orderMsg.startsWith("userName")) { // 유저 정보를 받아오는 명령어
						String[] userName = orderMsg.split(":");
						userId.add(userName[1]);
					}
					if(orderMsg.startsWith("deleteroom")) { // 방 삭제 하라는 명령어
						String[] orderData = orderMsg.split(":");
						System.out.println("서버 삭제 명령 받음");
						for(int i = 0; i < roomName.size();i++) {
							if(roomName.get(i).equals(orderData[1]) && createUser.get(i).equals(orderData[2])) {
								System.out.println(i + " 번째 방 삭제");
								broadCast(i);
								roomName.remove(i); // i 번째 방 삭제
								createUser.remove(i); // i 번째 방 만든 유저 정보 삭제
							} else {
								service.println("error:wrongUser");
							}
						}
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	
	
	public static void main(String[] args) {
		new Server2();
	}
	
	
	
}
