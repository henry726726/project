import React, { useState } from "react";
import PromptForm from "../components/PromptForm";
import ImageComposer from "../components/ImageComposer";

export default function Main() {
  const [selectedText, setSelectedText] = useState("");
  const [selectedImage, setSelectedImage] = useState(null);

  return (
    <div className="p-4 max-w-3xl mx-auto">
      <PromptForm
        onTextSelect={(text) => setSelectedText(text)}
        onImageSelect={(img) => setSelectedImage(img)}
      />
      {selectedText && selectedImage && (
        <ImageComposer text={selectedText} imageFile={selectedImage} />
      )}
    </div>
  );
}
