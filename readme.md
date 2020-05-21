# PowerBench
Automatic Energy Consumption Measuring Platform

## Hardware Requirements
1. a digital power supply or multimeter that supports SCPI command;
2. for standalone_ver, a STM32F7 neocleo board or discovery board is required;
3. for simplified_auxiliary_ver, a USB_TTL module (such as CP210x) is required;

## Software Requirements
1. Developed and only tested on Eclipse JUNO (Version: 4.2.2 Build id: M20130204-1200)
2. Java 8 Runtime Environment required;
3. Java 7 Runtime Environment compatible, but assertion must be enabled (-ea parameter required);
4. rxtxComm required.