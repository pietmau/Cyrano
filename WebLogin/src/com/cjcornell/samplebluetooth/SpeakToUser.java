package com.cjcornell.samplebluetooth;

public class SpeakToUser 
{
    private String tooManyUsers = "There are too many users to speak. Please see the screen and select a user.";
    private String nameMessage; 
    private String phoneMessage;
    private String emailMessage;
    
    
    public void speakAboutUser(String firstName, String lastName, String email, String phoneNumber)
    {
        nameMessage += "User " + firstName + "   " + lastName + "is nearby.";
        AudioMethods.textToSpeech(null, "Would you like to hear more information?");
        //Need to add logic to break from the method if the answer is no. 
        //Code for asking user what they want to here goes here. Can be text to speech or speech to text.
        phoneMessage += firstName + "'s phonenumber is: " + phoneNumber; 
        emailMessage += firstName + "'s email is " + email;
        
        AudioMethods.textToSpeech(null, nameMessage);
        AudioMethods.textToSpeech(null, phoneMessage);
        AudioMethods.textToSpeech(null, emailMessage);
    }
    
    //Here I am assuming an array of users. I am assuming that we will have a user object eventually as well. 
    public void speakListofUsers(String[] users)
    {
        if(users.length > 3)
        {
            AudioMethods.textToSpeech(null, tooManyUsers);
        }
        for(int i=0; i < users.length; i++)
        {
            //Here we iterate though the list to speak about all the found users. If it is less than 3. 
            
            //speakAboutUser(users[i].firstName, users[i].lastName, users[i].email, users[i].phoneNumber);
        }
    }

}
