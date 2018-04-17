# smiledispenser

Se você estiver vendo essa url, este eh um equipamento roubado!
Entre em contato com talktoleao@gmail.com com informaçoes sobre sua localizaçao.



## Equipment

* Raspiberry Pi 3 + 5V micro-usb power supply
* [Raspiberry Pi Touchscreen Display](https://www.raspberrypi.org/products/raspberry-pi-touch-display/)
* [Raspiberry Pi Camera module](https://www.raspberrypi.org/products/camera-module-v2/) with [2 meters flex cable](https://www.amazon.com/Adafruit-Flex-Cable-Raspberry-Camera/dp/B00XW2NCKS)
* Speaker (anyone with 3.5mm jack)
* [1 channel Relay Module](https://www.amazon.com/Tolako-Arduino-Indicator-Channel-Official/dp/B00VRUAHLE)
* [Candy Dispenser machine](https://www.amazon.com/Motion-Activated-Candy-Dispenser-candy-5/dp/B00AUZ4F0G/)
* 5V power supply (to use with the machine)


The machine came with a motor. So I just wired the relay module directly to the motor and use the Pi's GPIO to control it.


## Setup the Display:

To make the touch works you need to connect some pins directly to the raspi. You can use [this tutorial](http://www.makeuseof.com/tag/setup-raspberry-pi-touchscreen/) to connect the pins correctly.
