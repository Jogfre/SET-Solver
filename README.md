# SET-Solver
An android App that utilizes the camera in order to provide solutions for the game SET.

## Short description
The project uses OpenCV in order to process the camera feed in real-time in order to detect the cards in an image.
Once a card has been detected, it also uses OpenCV in order to crop out the card and warp the perspective of the card to always be top-down.

After that, the cropped card image is passed to a Machine-Learning model in order to classify what properties the card has.

Finally, once all cards has been classified, another algorithm determines any SETs that might be present in the image and then presents them to the user.

## Dependencies
- OpenCV, is not included in the repository and must be installed separately.
