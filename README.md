# Light HTTP Server

A simple HTTP web server built in Java to better understand networking, sockets, and how servers handle client requests.

## What This Project Does

This project listens for client connections, reads HTTP requests, and sends back HTTP responses. It is helping me learn how web servers work at a lower level instead of only using higher-level frameworks.

## Features

- Handles basic HTTP requests
- Uses Java sockets for client-server communication
- Sends HTTP response headers
- Serves files to the browser
- Includes room for future improvements like logging and multithreading

### Technologies Used

- Java
- VS Code
- GitHub

## How to Run

1. Clone the repository
2. Open the project in VS Code
3. Compile the Java file
4. Run the server
5. Open a browser and connect to the server using the correct localhost port

Example:

```bash
javac MyWebServer.java
java MyWebServer
